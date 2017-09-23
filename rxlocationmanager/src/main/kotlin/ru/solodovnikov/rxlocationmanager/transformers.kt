package ru.solodovnikov.rxlocationmanager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import rx.Completable
import rx.Observable
import rx.Single
import rx.Subscription
import java.util.*

interface TransformerSingle<T, R> {
    fun transform(upstream: Single<T>): Single<R>
}

interface TransformerObservable<T, R> {
    fun transform(upstream: Observable<T>): Observable<R>
}

interface TransformerCompletable {
    fun transform(upstream: Completable): Completable
}

interface Transformer<T, R> : TransformerSingle<T, R>, TransformerObservable<T, R>, TransformerCompletable

/**
 * Transformer used to request runtime permissions
 *
 * Call [RxLocationManager.onRequestPermissionsResult] inside your [android.app.Activity.onRequestPermissionsResult]
 * to get request permissions results in the transformer.
 */
open class PermissionTransformer<T>(context: Context,
                                    private val rxLocationManager: RxLocationManager,
                                    callback: BasePermissionTransformer.PermissionCallback
) : BasePermissionTransformer(context, callback), Transformer<T, T> {


    override fun transform(upstream: Single<T>): Single<T> =
            checkPermissions().andThen(upstream)

    override fun transform(upstream: Observable<T>): Observable<T> =
            checkPermissions().andThen(upstream)

    override fun transform(upstream: Completable): Completable =
            checkPermissions().andThen(upstream)

    /**
     * Construct [Completable] which check runtime permissions
     */
    protected open fun checkPermissions(): Completable =
            Completable.fromEmitter { emitter ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val deniedPermissions = getDeniedPermissions()

                    if (deniedPermissions.isNotEmpty()) {
                        //wait until user approve permissions or dispose action
                        subscribeToPermissionUpdate {
                            val resultPermissions = it.first
                            val resultPermissionsResults = it.second
                            if (!Arrays.equals(resultPermissions, deniedPermissions) ||
                                    resultPermissionsResults
                                            .find { it == PackageManager.PERMISSION_DENIED } != null) {
                                emitter.onError(SecurityException("User denied permissions: ${deniedPermissions.asList()}"))
                            } else {
                                emitter.onCompleted()
                            }
                        }.apply { emitter.setCancellation { unsubscribe() } }

                        callback.requestPermissions(deniedPermissions)
                    } else {
                        emitter.onCompleted()
                    }
                } else {
                    emitter.onCompleted()
                }
            }

    /**
     * Subscribe to request permissions result
     */
    protected fun subscribeToPermissionUpdate(onUpdate: (Pair<Array<out String>, IntArray>) -> Unit): Subscription =
            rxLocationManager.subscribeToPermissionUpdate(onUpdate)
}

/**
 * Transformer used to ignore any described error type.
 *
 * @param errorsToIgnore if empty, then ignore all errors, otherwise just described types.
 */
class IgnoreErrorTransformer<T>(vararg errorsToIgnore: Class<out Throwable>
) : Transformer<T, T> {
    private val toIgnore: Array<out Class<out Throwable>> = errorsToIgnore

    override fun transform(upstream: Single<T>): Single<T> =
            upstream.onErrorResumeNext {
                if (toIgnore.isEmpty() || toIgnore.contains(it.javaClass)) {
                    IgnorableException()
                } else {
                    it
                }.let { Single.error<T>(it) }
            }

    override fun transform(upstream: Observable<T>): Observable<T> =
            upstream.onErrorResumeNext {
                if (toIgnore.isEmpty() || toIgnore.contains(it.javaClass)) {
                    Observable.empty<T>()
                } else {
                    Observable.error<T>(it)
                }
            }

    override fun transform(upstream: Completable): Completable =
            upstream.onErrorResumeNext {
                if (toIgnore.isEmpty() || toIgnore.contains(it.javaClass)) {
                    Completable.complete()
                } else {
                    Completable.error(it)
                }
            }
}