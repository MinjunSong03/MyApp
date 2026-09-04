package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.example.myapp.auth.viewmodel.ManageMyUiState
import org.example.myapp.auth.viewmodel.ManageMyViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.ui.card.BlockedUserCard

@Composable
fun ManageMyScreen(
    viewModel: ManageMyViewModel= koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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


    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadMyBlockedUser(isRefresh = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is ManageMyUiState.Loading -> {
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ManageMyUiState.Success -> {
                if (state.users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "차단한 사용자가 없습니다.",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.users, key = { it.id }) { user ->
                            BlockedUserCard(
                                user = user,
                                onUnblockUserClick = { viewModel.unblockUser(user.id) }
                            )
                        }
                        if (!state.isLast) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}