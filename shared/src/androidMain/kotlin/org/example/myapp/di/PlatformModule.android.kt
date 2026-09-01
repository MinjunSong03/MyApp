package org.example.myapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import okio.Path.Companion.toPath
import org.example.myapp.auth.platform.AndroidAuthService
import org.example.myapp.auth.platform.AuthService
import org.example.myapp.auth.local.DATASTORE_FILE_NAME
import org.example.myapp.util.getAsyncImageLoader
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AuthService> { AndroidAuthService(get()) }
    single<DataStore<Preferences>> {
        val context: Context = get()
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath.toPath()
            }
        )
    }
    single<ImageLoader> { getAsyncImageLoader(get()) }
}