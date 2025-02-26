package com.example.treasurehunt_ar.ui.utils

import android.app.Activity
import android.widget.Toast
import com.example.treasurehunt_ar.AUTH_PORT
import com.example.treasurehunt_ar.DATABASE_PORT
import com.example.treasurehunt_ar.LOCALHOST
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

fun configureFirebaseServices() {
    Firebase.auth.useEmulator(LOCALHOST, AUTH_PORT)
    Firebase.database.useEmulator(LOCALHOST, DATABASE_PORT)
}

fun checkGooglePlayServicesForAR(
    activity: Activity
) {
    // Ensure that Google Play Services for AR and ARCore device profile data are installed and up to date.
    try {
        when (ArCoreApk.getInstance().requestInstall(activity, true)) {
            ArCoreApk.InstallStatus.INSTALLED -> {
                // Success, do nothing.
            }
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                // When this method returns `INSTALL_REQUESTED`:
                // 1. ARCore pauses this activity.
                // 2. ARCore prompts the user to install or update Google Play
                //    Services for AR (market://details?id=com.google.ar.core).
                // 3. ARCore downloads the latest device profile data.
                // 4. ARCore resumes this activity. The next invocation of
                //    requestInstall() will either return `INSTALLED` or throw an
                //    exception if the installation or update did not succeed.
                return
            }
        }
    } catch (e: UnavailableUserDeclinedInstallationException) {
        // Display an appropriate message to the user and return gracefully.
        Toast.makeText(activity, "TODO: handle exception $e", Toast.LENGTH_LONG)
            .show()
        return
    } catch (e: Exception) {  // Current catch statements cover developer error codes.
        Toast.makeText(activity, "TODO: handle exception $e", Toast.LENGTH_LONG)
            .show()
        return
    }
}

