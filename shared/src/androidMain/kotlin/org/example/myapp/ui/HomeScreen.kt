package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.myapp.auth.viewmodel.HomeViewModel
import org.example.myapp.shared.R
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.ui.item.PostFeedList
import org.example.myapp.util.AndroidVideoPlayerManager
import org.koin.compose.viewmodel.koinViewModel

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

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        videoManager.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        videoManager.pause()
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
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
            PostFeedList(
                posts = uiState.posts,
                isInitialLoading = uiState.isInitialLoading,
                isRefreshing = uiState.isRefreshing,
                isLast = uiState.isLast,
                emptyMessage = "아래로 당겨 새로고침해 보세요!",
                videoManager = videoManager,
                listState = listState,
                onRefresh = { viewModel.loadHomeFeed(isRefresh = true) },
                onLoadMore = { viewModel.loadHomeFeed(isRefresh = false) },
                onCardClick = onNavigateToPostDetail,
                onProfileClick = onNavigateToProfileClick,
                onEditClick = onNavigateToEditPost,
                onDeleteClick = { activeDialog = HomeDialog.DeletePost(it) },
                onHideClick = { activeDialog = HomeDialog.HidePost(it) },
                onBlockClick = { activeDialog = HomeDialog.BlockUser(it) },
                onReportPostClick = { activeDialog = HomeDialog.ReportPost(it) },
                onReportUserClick = { activeDialog = HomeDialog.ReportUser(it) }
            )

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