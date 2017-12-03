package ru.solodovnikov.rxlocationmanager.sample

import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.reactivex.Maybe
import io.reactivex.Single
import ru.solodovnikov.rx2locationmanager.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), PermissionCaller, ForResultCaller {
    private val rxLocationManager: RxLocationManager by lazy { RxLocationManager(this) }
    private val locationRequestBuilder: LocationRequestBuilder by lazy { LocationRequestBuilder(rxLocationManager) }

    private val enableLocationBehavior: Behavior by lazy { EnableLocationBehavior.resolve(this, REQUEST_CODE_LOCATION_SETTINGS, rxLocationManager, this) }
    private val permissionBehavior: Behavior by lazy { PermissionBehavior(this, rxLocationManager, this) }

    private val coordinatorLayout by lazy { findViewById<CoordinatorLayout>(R.id.root) }

    private var checkPermissions = false
    private var enableLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.check_permissions -> {
                item.isChecked = !item.isChecked
                checkPermissions = item.isChecked
                return true
            }
            R.id.enable_location -> {
                item.isChecked = !item.isChecked
                enableLocation = item.isChecked
                return true
            }
            R.id.last_network -> {
                requestLastNetworkLocation()
                return true
            }
            R.id.last_network_minute_old -> {
                requestLastNetworkOneMinuteOldLocation()
                return true
            }
            R.id.request_location -> {
                requestLocation()
                return true
            }
            R.id.complicated_request_location -> {
                requestBuild()
                return true
            }
            R.id.complicated_request_location_ignore_error -> {
                requestBuildIgnoreSecurityError()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            rxLocationManager.onActivityResult(resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            rxLocationManager.onRequestPermissionsResult(permissions, grantResults)
        }
    }

    override fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_LOCATION_PERMISSIONS)
    }

    private fun getBehaviors() =
            mutableListOf<Behavior>().also {
                if (checkPermissions) {
                    it.add(permissionBehavior)
                }
                if (enableLocation) {
                    it.add(enableLocationBehavior)
                }
            }.toTypedArray()

    private fun requestLastNetworkLocation() {
        rxLocationManager.getLastLocation(LocationManager.NETWORK_PROVIDER, behaviors = *getBehaviors())
                .testSubscribe("requestLastNetworkLocation")
    }

    private fun requestLastNetworkOneMinuteOldLocation() {
        rxLocationManager.getLastLocation(LocationManager.NETWORK_PROVIDER,
                LocationTime(1, TimeUnit.MINUTES),
                *getBehaviors())
                .testSubscribe("requestLastNetworkOneMinuteOldLocation")
    }

    private fun requestLocation() {
        rxLocationManager.requestLocation(LocationManager.NETWORK_PROVIDER,
                LocationTime(15, TimeUnit.SECONDS),
                *getBehaviors())
                .testSubscribe("requestLocation")
    }

    private fun requestBuild() {
        val behaviors = getBehaviors()

        locationRequestBuilder
                .addLastLocation(LocationManager.NETWORK_PROVIDER, LocationTime(30, TimeUnit.MINUTES), *behaviors)
                .addRequestLocation(LocationManager.NETWORK_PROVIDER, LocationTime(15, TimeUnit.SECONDS), *behaviors)
                .setDefaultLocation(Location(LocationManager.PASSIVE_PROVIDER))
                .create()
                .testSubscribe("requestBuild")
    }

    private fun requestBuildIgnoreSecurityError() {
        val ignoreError = IgnoreErrorBehavior(SecurityException::class.java)

        locationRequestBuilder
                .addLastLocation(LocationManager.NETWORK_PROVIDER, LocationTime(30, TimeUnit.MINUTES), ignoreError)
                .addRequestLocation(LocationManager.NETWORK_PROVIDER, LocationTime(15, TimeUnit.SECONDS), ignoreError)
                .setDefaultLocation(Location(LocationManager.PASSIVE_PROVIDER))
                .create()
                .testSubscribe("requestBuild")
    }

    private fun showSnackbar(text: CharSequence) {
        Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_SHORT)
                .show()
    }

    private fun showLocationMessage(location: Location?, methodName: String) {
        showSnackbar("$methodName Success: ${location?.toString() ?: "Empty location"}")
    }

    private fun showErrorMessage(throwable: Throwable, methodName: String) {
        showSnackbar("$methodName Error: ${throwable.message}")
    }

    private fun Maybe<Location>.testSubscribe(methodName: String) {
        subscribe({ showLocationMessage(it, methodName) },
                { showErrorMessage(it, methodName) },
                { showSnackbar("$methodName Completed") })
    }

    private fun Single<Location>.testSubscribe(methodName: String) {
        subscribe({ showLocationMessage(it, methodName) },
                { showErrorMessage(it, methodName) })
    }

    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSIONS = 150
        private const val REQUEST_CODE_LOCATION_SETTINGS = 151
    }
}
