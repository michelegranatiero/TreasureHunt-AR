package com.example.treasurehunt_ar.model.service.module

import com.example.treasurehunt_ar.model.service.AccountService
import com.example.treasurehunt_ar.model.service.impl.AccountServiceImpl

interface ServiceModule {
    val accountService: AccountService
    // val storageService: StorageService
}

class ServiceModuleImpl : ServiceModule {
    override val accountService: AccountService by lazy {
        AccountServiceImpl()
    }

    // override val storageService: StorageService by lazy {
    //     StorageServiceImpl()
    // }
}