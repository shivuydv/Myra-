package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class MyraPermissionStatus(
    val microphone: Boolean,
    val contacts: Boolean,
    val accessibility: Boolean
)

class MyraPermissionManager(private val activity: Activity) {

    companion object {
        const val REQUEST_MIC = 1001
        const val REQUEST_CONTACTS = 1002
    }

    fun hasMicrophone(): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun requestMicrophone() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_MIC
        )
    }

    fun hasContacts(): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    fun requestContacts() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQUEST_CONTACTS
        )
    }

    fun isAccessibilityEnabled(): Boolean =
        MyraAccessibilityService.instance != null

    fun openAccessibilitySettings() {
        activity.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }

    fun getStatus() =
        MyraPermissionStatus(
            hasMicrophone(),
            hasContacts(),
            isAccessibilityEnabled()
        )
}
