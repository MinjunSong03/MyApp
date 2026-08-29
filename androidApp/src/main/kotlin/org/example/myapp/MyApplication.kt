package org.example.myapp

import android.app.Application
import org.example.myapp.auth.platform.initKakaoSdk
import org.example.myapp.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@MyApplication)
        }

        initKakaoSdk(this, BuildConfig.KAKAO_APP_KEY)
    }
}