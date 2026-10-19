package com.baz.controller

import android.app.Application
import com.baz.controller.connection.ConnectionManager
import com.baz.controller.haptics.HapticsManager
import com.baz.controller.protocol.SecurityManager
import com.baz.controller.repository.TemplateRepository

class BazApplication : Application() {

    lateinit var securityManager: SecurityManager
        private set

    lateinit var hapticsManager: HapticsManager
        private set

    lateinit var connectionManager: ConnectionManager
        private set

    lateinit var templateRepository: TemplateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        securityManager = SecurityManager(this)
        hapticsManager = HapticsManager(this)
        connectionManager = ConnectionManager(this, securityManager)
        templateRepository = TemplateRepository(this)
    }

    companion object {
        lateinit var instance: BazApplication
            private set
    }
}
