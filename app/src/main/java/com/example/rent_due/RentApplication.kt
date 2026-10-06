package com.example.rent_due

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.example.rent_due.data.local.RentDatabase
import com.example.rent_due.data.repository.RentRepository

class RentApplication : Application() {
    val database by lazy { RentDatabase.getDatabase(this) }
    val repository by lazy { RentRepository(database.rentDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize Google Mobile Ads SDK on app launch
        MobileAds.initialize(this) {}
    }
}
