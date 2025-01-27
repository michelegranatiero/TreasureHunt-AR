package com.example.ar_firstapp.model.service.module

import com.example.ar_firstapp.model.service.AccountService
import com.example.ar_firstapp.model.service.impl.AccountServiceImpl

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