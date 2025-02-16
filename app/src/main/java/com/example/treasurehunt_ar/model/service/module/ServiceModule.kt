package com.example.treasurehunt_ar.model.service.module

import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.model.service.GamingService
import com.example.treasurehunt_ar.model.service.impl.AccountServiceImpl
import com.example.treasurehunt_ar.model.service.impl.GamingServiceImpl

interface ServiceModule {
    val accountService: AccountService
    val gamingService: GamingService
}

class ServiceModuleImpl : ServiceModule {
    override val accountService: AccountService by lazy {
        AccountServiceImpl()
    }

    override val gamingService: GamingService by lazy {
        GamingServiceImpl(accountService)
    }
}
