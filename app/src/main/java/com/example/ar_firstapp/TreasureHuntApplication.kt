package com.example.ar_firstapp

import android.app.Application
import com.example.ar_firstapp.model.service.module.ServiceModule
import com.example.ar_firstapp.model.service.module.ServiceModuleImpl

class TreasureHuntApplication: Application() {
    companion object {
        lateinit var serviceModule: ServiceModule
    }

    override fun onCreate() {
        super.onCreate()
        serviceModule = ServiceModuleImpl()
    }
}