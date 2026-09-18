package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.myapp.auth.viewmodel.HomeUiState
import org.example.myapp.auth.viewmodel.HomeViewModel
import org.example.myapp.ui.card.PostCard
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.AppPullToRefreshBox
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.util.AndroidVideoPlayerManager
import org.example.myapp.shared.R

private sealed interface HomeDialog {
    data class DeletePost(val postId: Long): HomeDialog
    data class HidePost(val postId: Long): HomeDialog
    data class BlockUser(val userId: Long): HomeDialog
    data class ReportPost(val postId: Long): HomeDialog
    data class ReportUser(val userId: Long): HomeDialog
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    listState: LazyListState,
    onNavigateToEditPost: (Long) -> Unit,
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToProfileClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf<HomeDialog?>(null) }
    val currentUiState by rememberUpdatedState(uiState)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) null
            else {
                when {
                    !listState.canScrollBackward -> {
                        visibleItems.firstOrNull()?.index
                    }

                    !listState.canScrollForward -> {
                        val postsCount = currentUiState.posts.size
                        visibleItems.lastOrNull { it.index < postsCount }?.index
                    }

                    else -> {
                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                        visibleItems.minByOrNull { item ->
                            val itemCenter = item.offset + item.size / 2
                            kotlin.math.abs(itemCenter - viewportCenter)
                        }?.index
                    }
                }
            }
        }.collect { centerIndex ->
            val state = currentUiState
            if (centerIndex != null && state.posts.isNotEmpty()) {
                val posts = state.posts
                val targetPost = posts.getOrNull(centerIndex)
                val videoUrl = targetPost?.videoUrl
                if (!videoUrl.isNullOrEmpty() && videoManager.currentPlayingUrl.value != videoUrl) {
                    videoManager.play(videoUrl)
                } else if (videoUrl.isNullOrEmpty()) {
                    videoManager.pause()
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { videoManager.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { videoManager.pause() }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadHomeFeed(isRefresh = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppTopBar(
                iconPainter = painterResource(R.drawable.ic_launcher_foreground),
                title = "----",
                onBackClick = null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = {
                    videoManager.stop()
                    viewModel.loadHomeFeed(isRefresh = true)
                            },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isInitialLoading && uiState.posts.isEmpty() -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.isEmpty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "아래로 스와이프하여 게시물을 로드해 보세요!",
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
                            items(uiState.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onCardClick = onNavigateToPostDetail,
                                    onProfileClick = onNavigateToProfileClick,
                                    videoManager = videoManager,
                                    onEditClick = onNavigateToEditPost,
                                    onDeleteClick = { activeDialog = HomeDialog.DeletePost(it) },
                                    onUnhidePostClick = {},
                                    onHidePostClick = { activeDialog = HomeDialog.HidePost(it) },
                                    onBlockUserClick = { activeDialog = HomeDialog.BlockUser(it) },
                                    onReportPostClick = { activeDialog = HomeDialog.ReportPost(it) },
                                    onReportUserClick = { activeDialog = HomeDialog.ReportUser(it) }
                                )
                            }
                            if (!uiState.isLast) {
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

            when(val dialog = activeDialog) {
                is HomeDialog.DeletePost -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "이 게시물 삭제") },
                        text = { Text(text = "이 게시물을 삭제하시겠습니까?\n게시물을 삭제 후 복구는 불가능합니다.") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePost(dialog.postId)
                                    activeDialog = null
                                }
                            ) {
                                Text(
                                    text = "삭제",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeDialog = null }) {
                                Text(text = "취소")
                            }
                        }
                    )
                }

                is HomeDialog.HidePost -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "게시물 숨기기") },
                        text = { Text(text = "이 게시물을 숨기시겠습니까?") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.hidePost(dialog.postId)
                                    activeDialog = null
                                }
                            ) {
                                Text(
                                    text = "숨기기",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeDialog = null }) {
                                Text(text = "취소")
                            }
                        }
                    )
                }
                is HomeDialog.BlockUser -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "이 사용자 차단") },
                        text = { Text(text = "이 사용자를 차단하시겠습니까?\n피드에서 해당 사용자의 모든 글이 즉시 숨겨집니다.") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.blockUser(dialog.userId)
                                    activeDialog = null
                                }
                            ) {
                                Text(
                                    text = "차단",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeDialog = null }) {
                                Text(text = "취소")
                            }
                        }
                    )
                }
                is HomeDialog.ReportPost -> {
                    ReportDialog(
                        onDismiss = { activeDialog = null },
                        onConfirm = { reportReason, detail ->
                            viewModel.reportPost(dialog.postId, reportReason, detail)
                            activeDialog = null
                        }
                    )
                }
                is HomeDialog.ReportUser -> {
                    ReportDialog(
                        onDismiss = { activeDialog = null },
                        onConfirm = { reportReason, detail ->
                            viewModel.reportUser(dialog.userId, reportReason, detail)
                            activeDialog = null
                        }
                    )
                }
                null -> Unit
            }
        }
    }
}