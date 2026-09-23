package org.example.myapp.ui.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.myapp.auth.network.UserResponse
import org.example.myapp.ui.card.UserCard

@Composable
fun UserList(
    users: List<UserResponse>,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    isLast: Boolean,
    emptyMessage: String,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = true,
    onLikeClick: ((Long) -> Unit)? = null,
    onUnblockClick: ((Long) -> Unit)? = null,
    onBlockClick: ((Long) -> Unit)? = null,
    onReportClick: ((Long) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLast && !isRefreshing && !isInitialLoading) {
            onLoadMore()
        }
    }

    AppPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isInitialLoading && users.isEmpty() -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            !isInitialLoading && users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        UserCard(
                            user = user,
                            onProfileClick = onProfileClick,
                            isLiked = isLiked,
                            onLikeClick = onLikeClick,
                            onUnblockClick = onUnblockClick,
                            onBlockClick = onBlockClick,
                            onReportClick = onReportClick
                        )
                    }
                    if (!isLast) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}