package com.example.treasurehunt_ar

import android.app.Application
import com.example.treasurehunt_ar.model.service.module.ServiceModule
import com.example.treasurehunt_ar.model.service.module.ServiceModuleImpl

class TreasureHuntApplication: Application() {
    companion object {
        lateinit var serviceModule: ServiceModule
    }

    override fun onCreate() {
        super.onCreate()
        serviceModule = ServiceModuleImpl()
    }
}