package ru.solodovnikov.rxlocationmanager

import android.location.Location

class ProviderDisabledException(val provider: String) : Throwable("The $provider provider is disabled")

class ElderLocationException(val location: Location) : Throwable("The location is too old")

/**
 * Throwed when Android [provider] has no last location
 */
class ProviderHasNoLastLocationException(val provider: String) : Throwable("The $provider has no last location")