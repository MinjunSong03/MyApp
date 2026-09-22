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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.ui.card.PostCard
import org.example.myapp.util.AndroidVideoPlayerManager

@Composable
fun PostFeedList(
    posts: List<PostResponse>,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    isLast: Boolean,
    emptyMessage: String,
    videoManager: AndroidVideoPlayerManager,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCardClick: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    onLikeClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onHideClick: (Long) -> Unit,
    onUnhideClick: ((Long) -> Unit)? = null,
    onBlockClick: ((Long) -> Unit)? = null,
    onReportPostClick: ((Long) -> Unit)? = null,
    onReportUserClick: ((Long) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val currentPosts by rememberUpdatedState(posts)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) null
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - viewportCenter) }?.index
            }
        }.collect { centerIndex ->
            if (centerIndex != null && currentPosts.isNotEmpty()) {
                val videoUrl = currentPosts.getOrNull(centerIndex)?.videoUrl
                if (!videoUrl.isNullOrEmpty() && videoManager.currentPlayingUrl.value != videoUrl) {
                    videoManager.play(videoUrl)
                } else if (videoUrl.isNullOrEmpty()) {
                    videoManager.pause()
                }
            }
        }
    }

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
        onRefresh = {
            videoManager.stop()
            onRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isInitialLoading && posts.isEmpty() -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            !isInitialLoading && posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            videoManager = videoManager,
                            onCardClick = onCardClick,
                            onProfileClick = onProfileClick,
                            onLikeClick = onLikeClick,
                            onEditClick = onEditClick,
                            onDeleteClick = onDeleteClick,
                            onHidePostClick = onHideClick,
                            onUnhidePostClick = { onUnhideClick?.invoke(it) },
                            onBlockUserClick = { onBlockClick?.invoke(it) },
                            onReportPostClick = { onReportPostClick?.invoke(it) },
                            onReportUserClick = { onReportUserClick?.invoke(it) }
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