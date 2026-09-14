package org.example.myapp.ui.license

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext

@Composable
actual fun OpenSourceLicenseScreen(modifier: Modifier) {
    val context = LocalContext.current

    val libraries = remember(context) {
        Libs.Builder().withContext(context).build()
    }

    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize()
    )
}