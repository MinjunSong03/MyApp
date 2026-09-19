package org.example.myapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import org.example.myapp.auth.platform.AndroidAuthService
import org.example.myapp.auth.platform.AuthService
import org.example.myapp.auth.local.DATASTORE_FILE_NAME
import org.example.myapp.auth.local.PostDatabase
import org.example.myapp.util.AndroidVideoPlayerManager
import org.example.myapp.util.getAsyncImageLoader
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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
    viewModel { AndroidVideoPlayerManager(get()) }
    single<PostDatabase> {
        val context: Context = get()
        val dbFile = context.getDatabasePath("post.db")

        Room.databaseBuilder<PostDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}