package ru.solodovnikov.rxlocationmanager

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle

interface ForResultCaller {
    /**
     * @see android.app.Activity.startActivityForResult
     * @see android.app.Fragment.startActivityForResult
     */
    fun startActivityForResult(data: Intent, requestCode: Int)

    /**
     * @see android.app.Activity.startIntentSenderForResult
     * @see android.app.Fragment.startIntentSenderForResult
     */
    fun startIntentSenderForResult(intent: IntentSender, requestCode: Int,
                                   fillInIntent: Intent?, flagsMask: Int, flagsValues: Int,
                                   extraFlags: Int, options: Bundle?)
}

interface PermissionCaller {
    /**
     * Called to request permissions
     * @see android.app.Activity.requestPermissions
     * @see android.app.Fragment.requestPermissions
     */
    fun requestPermissions(permissions: Array<String>)
}