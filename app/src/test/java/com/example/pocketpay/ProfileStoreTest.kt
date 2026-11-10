package com.example.pocketpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pocketpay.data.ProfileStore
import com.example.pocketpay.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileStoreTest {

    private lateinit var context: Context
    private val defaults = UserProfile("Nasabah Utama", "0812-0000", "a@b.com")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pocketpay_profile", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun load_withNothingSaved_returnsDefaults() {
        assertEquals(defaults, ProfileStore(context).load(defaults))
    }

    @Test
    fun saveThenLoad_survivesNewInstance() {
        ProfileStore(context).save(UserProfile("Budi", "0899-1111", "budi@x.com"))
        // A fresh instance simulates an app restart.
        assertEquals(UserProfile("Budi", "0899-1111", "budi@x.com"), ProfileStore(context).load(defaults))
    }
}
