package com.android.billing

import android.app.Activity
import android.content.Context
import com.android.billing.models.FeatureType
import com.android.billing.models.ProductDetails
import com.android.billing.models.ProductId
import com.android.billing.models.ProductType
import com.android.billing.models.translate
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

private typealias ResponseListener<T> = (Result<T>) -> Unit

interface BillingClientProvider {

    fun initialize(
        callback: (Unit) -> Unit,
        onFailed: () -> Unit
    )

    fun dispose()

    fun verifyFeatureSupported(featureType: FeatureType, listener: ResponseListener<Boolean>)
    fun verifyFeaturesSupported(
        featureTypes: List<FeatureType>,
        listener: ResponseListener<Map<FeatureType, Boolean>>
    )

    fun queryProductDetails(
        productId: ProductId,
        productType: ProductType,
        listener: ResponseListener<ProductDetails>
    )

    fun queryProductDetailsList(
        productDetailsCommand: QueryProductDetailsCommand,
        productType: ProductType,
        listener: ResponseListener<List<ProductDetails>>
    )

    fun queryPurchases(productType: ProductType, listener: ResponseListener<List<Purchase>>)
    fun queryPurchaseHistory(productType: ProductType, listener: ResponseListener<List<Purchase>>)
    fun consumePurchase(purchase: Purchase, listener: ResponseListener<ConsumeResult>)
    fun acknowledgePurchase(purchase: Purchase, listener: ResponseListener<AcknowledgeResult>)
    fun launchBillingFlow(
        activity: Activity,
        command: PurchaseSingleCommand,
        listener: ResponseListener<SingleBillingFlowResult>
    )

    enum class State {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        DISPOSED,
        UNAVAILABLE,
    }
}

class BillingClientProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BillingClientProvider {

    private val initializeResponseListeners = mutableListOf<ResponseListener<Unit>>()
    private val compositeListener = CompositePurchasesUpdatedListener()

    private var billingClient: BillingClient? = null

    private var state = BillingClientProvider.State.DISCONNECTED

    override fun initialize(
        callback: (Unit) -> Unit,
        onFailed: () -> Unit
    ) {
        when (state) {
            BillingClientProvider.State.UNAVAILABLE -> {
                onFailed()
                Timber.d("BillingClient unavailable")
                return
            }

            BillingClientProvider.State.CONNECTED -> {
                callback.invoke(Unit)
                Timber.d("BillingClient already connected")
                return
            }

            BillingClientProvider.State.CONNECTING -> {
                Timber.d("BillingClient is connecting")
                return
            }

            BillingClientProvider.State.DISPOSED, BillingClientProvider.State.DISCONNECTED -> {
                state = BillingClientProvider.State.CONNECTING
                billingClient = BillingClient
                    .newBuilder(context)
                    .setListener(compositeListener)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                    )
                    .build()
            }
        }

        billingClient?.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    state = when (val response = billingResult.toResponse()) {
                        is BillingResponse.OK -> {
                            Timber.d("BillingClient connected")
                            initializeResponseListeners.forEach { it.invoke(Result.success(Unit)) }
                            callback.invoke(Unit)
                            BillingClientProvider.State.CONNECTED
                        }

                        is BillingResponse.BillingUnavailable -> {
                            Timber.d("BillingClient unavailable")
                            onFailed()
                            initializeResponseListeners.forEach {
                                it.invoke(
                                    Result.failure(
                                        InitializationFailedException(response)
                                    )
                                )
                            }
                            BillingClientProvider.State.UNAVAILABLE
                        }

                        else -> {
                            Timber.d("BillingClient connection failed: $response")
                            initializeResponseListeners.forEach {
                                it.invoke(
                                    Result.failure(
                                        InitializationFailedException(response)
                                    )
                                )
                            }
                            BillingClientProvider.State.DISPOSED
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.d("BillingClient disconnected")
                    state = BillingClientProvider.State.DISCONNECTED
                }
            },
        )
    }

    override fun dispose() {
        billingClient?.endConnection()
        billingClient = null
        state = BillingClientProvider.State.DISPOSED
    }

    override fun verifyFeatureSupported(
        featureType: FeatureType,
        listener: ResponseListener<Boolean>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        verifyFeaturesSupported(listOf(featureType)) { result ->
            result.fold(
                onSuccess = { listener.invoke(Result.success(it[featureType] ?: false)) },
                onFailure = {
                    if (it is VerifyFeaturesSupportedFailedException) {
                        listener.invoke(
                            Result.failure(
                                VerifyFeatureSupportedFailedException(
                                    response = billingResponse(BillingResponseCode.FEATURE_NOT_SUPPORTED),
                                    feature = featureType,
                                    isCalledAfterDispose = it.isCalledAfterDispose,
                                    isFailedOnInitialize = it.isFailedOnInitialize,
                                ),
                            ),
                        )
                    } else {
                        listener.invoke(Result.failure(it))
                    }
                },
            )
        }
    }

    override fun verifyFeaturesSupported(
        featureTypes: List<FeatureType>,
        listener: ResponseListener<Map<FeatureType, Boolean>>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }
        if (billingClient == null) {
            return
        }
        val resultSet = mutableMapOf<FeatureType, Boolean>()

        for (featureType in featureTypes) {
            when (val response =
                billingClient!!.isFeatureSupported(featureType.rawValue).toResponse()) {
                is BillingResponse.OK -> resultSet[featureType] = true
                is BillingResponse.FeatureNotSupported -> resultSet[featureType] = false
                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("verifyFeatureSupported: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(
                        Result.failure(
                            VerifyFeatureSupportedFailedException(
                                response,
                                featureType
                            )
                        )
                    )
                    return
                }

                else -> {
                    listener.invoke(
                        Result.failure(
                            VerifyFeatureSupportedFailedException(
                                response,
                                featureType
                            )
                        )
                    )
                    return
                }
            }
        }

        if (resultSet.values.all { it }) {
            listener.invoke(Result.success(resultSet))
        } else {
            listener.invoke(
                Result.failure(
                    VerifyFeaturesSupportedFailedException(
                        billingResponse(BillingResponseCode.FEATURE_NOT_SUPPORTED),
                        FeaturesSupportedResult(resultSet),
                    ),
                ),
            )
        }
    }

    override fun queryProductDetails(
        productId: ProductId,
        productType: ProductType,
        listener: ResponseListener<ProductDetails>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        queryProductDetailsList(
            QueryProductDetailsCommand(listOf(productId)),
            productType
        ) { result ->
            result.fold(
                onSuccess = {
                    if (it.isEmpty()) {
                        listener.invoke(
                            Result.failure(
                                QueryProductDetailsFailedException(
                                    billingResponse(BillingResponseCode.ITEM_UNAVAILABLE),
                                    productId
                                )
                            )
                        )
                    } else {
                        listener.invoke(Result.success(it.first()))
                    }
                },
                onFailure = { listener.invoke(Result.failure(it)) },
            )
        }
    }

    override fun queryProductDetailsList(
        productDetailsCommand: QueryProductDetailsCommand,
        productType: ProductType,
        listener: ResponseListener<List<ProductDetails>>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        billingClient?.queryProductDetailsAsync(
            productDetailsCommand.toQueryProductDetailsParams(
                productType
            )
        ) { result, products ->
            when (val response = result.toResponse()) {
                is BillingResponse.OK -> {
                    //listener.invoke(Result.success(products.translate()))
                }

                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("queryProductDetails: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(
                        Result.failure(
                            QueryProductDetailsListFailedException(
                                response,
                                productDetailsCommand
                            )
                        )
                    )
                }

                else -> {
                    listener.invoke(
                        Result.failure(
                            QueryProductDetailsListFailedException(
                                response,
                                productDetailsCommand
                            )
                        )
                    )
                }
            }
        }
    }

    override fun queryPurchases(
        productType: ProductType,
        listener: ResponseListener<List<Purchase>>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType.rawValue)
            .build()

        billingClient?.queryPurchasesAsync(params) { result, purchases ->
            when (val response = result.toResponse()) {
                is BillingResponse.OK -> {
                    listener.invoke(Result.success(purchases))
                }

                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("queryPurchases: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(Result.failure(QueryPurchasesFailedException(response)))
                }

                else -> {
                    listener.invoke(Result.failure(QueryPurchasesFailedException(response)))
                }
            }
        }
    }

    override fun queryPurchaseHistory(
        productType: ProductType,
        listener: ResponseListener<List<Purchase>>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType.rawValue)
            .build()

        billingClient?.queryPurchasesAsync(params) { result, purchases ->
            when (val response = result.toResponse()) {
                is BillingResponse.OK -> {
                    listener.invoke(Result.success(purchases))
                }

                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("queryPurchaseHistory: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(Result.failure(QueryPurchasesFailedException(response)))
                }

                else -> {
                    listener.invoke(Result.failure(QueryPurchasesFailedException(response)))
                }
            }
        }
    }

    override fun consumePurchase(
        purchase: Purchase,
        listener: ResponseListener<ConsumeResult>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.consumeAsync(params) { result, _ ->
            when (val response = result.toResponse()) {
                is BillingResponse.OK -> {
                    listener.invoke(Result.success(ConsumeResult(false, params)))
                }

                is BillingResponse.ItemNotOwned -> {
                    listener.invoke(Result.success(ConsumeResult(true, params)))
                }

                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("consumePurchase: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(
                        Result.failure(
                            ConsumePurchaseFailedException(
                                response,
                                params
                            )
                        )
                    )
                }

                else -> {
                    listener.invoke(
                        Result.failure(
                            ConsumePurchaseFailedException(
                                response,
                                params
                            )
                        )
                    )
                }
            }
        }
    }

    override fun acknowledgePurchase(
        purchase: Purchase,
        listener: ResponseListener<AcknowledgeResult>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { result ->
            when (val response = result.toResponse()) {
                is BillingResponse.OK -> {
                    listener.invoke(Result.success(AcknowledgeResult(params)))
                }

                is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                    Timber.d("acknowledgePurchase: service error. CODE=${response.code}")
                    state = BillingClientProvider.State.DISPOSED
                    listener.invoke(
                        Result.failure(
                            AcknowledgePurchaseFailedException(
                                response,
                                params
                            )
                        )
                    )
                }

                else -> {
                    listener.invoke(
                        Result.failure(
                            AcknowledgePurchaseFailedException(
                                response,
                                params
                            )
                        )
                    )
                }
            }
        }
    }

    override fun launchBillingFlow(
        activity: Activity,
        command: PurchaseSingleCommand,
        listener: ResponseListener<SingleBillingFlowResult>,
    ) {
        require(state == BillingClientProvider.State.CONNECTED) { "BillingClient is not connected" }

        val updatedListener = object : PurchasesUpdatedListener {
            override fun onPurchasesUpdated(
                result: BillingResult,
                purchases: MutableList<Purchase>?
            ) {
                val response = result.toResponse()
                val isError = response !is BillingResponse.OK
                val isLibraryError = response is BillingResponse.OK && purchases == null
                val isPurchaseHandled =
                    response is BillingResponse.OK && purchases != null && purchases.any {
                        it.products.contains(command.productId.value) && !it.isAcknowledged
                    }

                if (isError || isLibraryError || isPurchaseHandled) {
                    compositeListener.remove(this)
                }

                when (response) {
                    is BillingResponse.OK -> {
                        if (purchases == null) {
                            listener.invoke(
                                Result.failure(
                                    LaunchBillingFlowFailedException(
                                        response,
                                        command
                                    )
                                )
                            )
                            return
                        }

                        if (purchases.any { it.products.contains(command.productId.value) && !it.isAcknowledged }) {
                            listener.invoke(
                                Result.success(
                                    SingleBillingFlowResult(
                                        command,
                                        purchases
                                    )
                                )
                            )
                        }
                    }

                    is BillingResponse.ServiceDisconnected, is BillingResponse.ServiceError -> {
                        Timber.d("launchBillingFlow: service error. CODE=${response.code}")
                        state = BillingClientProvider.State.DISPOSED
                        listener.invoke(
                            Result.failure(
                                LaunchBillingFlowFailedException(
                                    response,
                                    command
                                )
                            )
                        )
                    }

                    else -> {
                        listener.invoke(
                            Result.failure(
                                LaunchBillingFlowFailedException(
                                    response,
                                    command
                                )
                            )
                        )
                    }
                }
            }
        }

        compositeListener.add(updatedListener)

        val launchBillingResponse = billingClient
            ?.launchBillingFlow(activity, command.toBillingFlowParams())
            ?.toResponse() ?: return

        if (launchBillingResponse !is BillingResponse.OK) {
            if (launchBillingResponse is BillingResponse.ServiceDisconnected || launchBillingResponse is BillingResponse.ServiceError) {
                Timber.d("launchBillingFlow: service error. CODE=${launchBillingResponse.code}")
                state = BillingClientProvider.State.DISPOSED
            }

            compositeListener.remove(updatedListener)
            listener.invoke(
                Result.failure(
                    LaunchBillingFlowFailedException(
                        launchBillingResponse,
                        command
                    )
                )
            )
        }
    }
}
