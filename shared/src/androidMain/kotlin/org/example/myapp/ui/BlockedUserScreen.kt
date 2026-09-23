package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.myapp.auth.viewmodel.BlockedUserViewModel
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.ui.item.UserList
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlockedUserScreen(
    viewModel: BlockedUserViewModel= koinViewModel(),
    onBack: () -> Unit,
    onNavigateToProfileClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyBlockedUser(isRefresh = true)
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMyBlockedUser(isRefresh = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "차단한 사용자 관리",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        UserList(
            users = uiState.users,
            isInitialLoading = uiState.isInitialLoading,
            isRefreshing = uiState.isRefreshing,
            isLast = uiState.isLast,
            emptyMessage = "차단한 사용자가 없습니다.",
            onRefresh = { viewModel.loadMyBlockedUser(isRefresh = true) },
            onLoadMore = { viewModel.loadMyBlockedUser(isRefresh = false) },
            onProfileClick = onNavigateToProfileClick,
            onUnblockClick = { viewModel.unblockUser(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}