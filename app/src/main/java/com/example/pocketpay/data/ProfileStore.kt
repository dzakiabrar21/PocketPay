package com.example.pocketpay.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(val name: String, val phone: String, val email: String)

/** Persists the editable user profile so it survives app restarts. */
@Singleton
class ProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(default: UserProfile): UserProfile = UserProfile(
        name = prefs.getString(KEY_NAME, null) ?: default.name,
        phone = prefs.getString(KEY_PHONE, null) ?: default.phone,
        email = prefs.getString(KEY_EMAIL, null) ?: default.email
    )

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_EMAIL, profile.email)
            .apply()
    }

    private companion object {
        const val PREFS = "pocketpay_profile"
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
    }
}
