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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.myapp.auth.viewmodel.LikedContentViewModel
import org.example.myapp.ui.card.LikedUserCard
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.AppPullToRefreshBox
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.ui.item.PostFeedList
import org.example.myapp.util.AndroidVideoPlayerManager
import org.koin.compose.viewmodel.koinViewModel

private sealed interface LikedContentDialog {
    data class DeletePost(val postId: Long) : LikedContentDialog
    data class HidePost(val postId: Long) : LikedContentDialog
    data class BlockUser(val userId: Long) : LikedContentDialog
    data class ReportPost(val postId: Long) : LikedContentDialog
    data class ReportUser(val userId: Long) : LikedContentDialog
}

@Composable
fun LikedContentScreen(
    viewModel: LikedContentViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    onNavigateToProfileClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var activeDialog by remember { mutableStateOf<LikedContentDialog?>(null) }

    val userListState = rememberLazyListState()
    val shouldLoadMoreUsers by remember {
        derivedStateOf {
            val totalItems = userListState.layoutInfo.totalItemsCount
            val lastVisibleIndex = userListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMoreUsers) {
        if (shouldLoadMoreUsers) {
            viewModel.loadLikedUsers(isRefresh = false)
        }
    }

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
                title = "좋아요",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("좋아요한 사용자") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("좋아요한 게시물") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    AppPullToRefreshBox(
                        isRefreshing = usersState.isRefreshing,
                        onRefresh = { viewModel.loadLikedUsers(isRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            usersState.isInitialLoading && usersState.users.isEmpty() -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            usersState.isEmpty -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "좋아요한 사용자가 없습니다.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = userListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(usersState.users, key = { it.id }) { user ->
                                        LikedUserCard(
                                            user = user,
                                            isLiked = true,
                                            onProfileClick = onNavigateToProfileClick,
                                            onLikeClick = { viewModel.toggleLikeUser(it) },
                                            onBlockUserClick = { activeDialog = LikedContentDialog.BlockUser(it) },
                                            onReportUserClick = { activeDialog = LikedContentDialog.ReportUser(it) }
                                        )
                                    }
                                    if (!usersState.isLast) {
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
                } else {
                    PostFeedList(
                        posts = postsState.posts,
                        isInitialLoading = postsState.isInitialLoading,
                        isRefreshing = postsState.isRefreshing,
                        isLast = postsState.isLast,
                        emptyMessage = "좋아요한 게시물이 없습니다.",
                        videoManager = videoManager,
                        onRefresh = { viewModel.loadLikedPosts(isRefresh = true) },
                        onLoadMore = { viewModel.loadLikedPosts(isRefresh = false) },
                        onCardClick = onNavigateToPostDetail,
                        onProfileClick = onNavigateToProfileClick,
                        onLikeClick = { viewModel.toggleLikePost(it) },
                        onEditClick = onNavigateToEditPost,
                        onDeleteClick = { activeDialog = LikedContentDialog.DeletePost(it) },
                        onHideClick = { activeDialog = LikedContentDialog.HidePost(it) },
                        onBlockClick = { activeDialog = LikedContentDialog.BlockUser(it) },
                        onReportPostClick = { activeDialog = LikedContentDialog.ReportPost(it) },
                        onReportUserClick = { activeDialog = LikedContentDialog.ReportUser(it) }
                    )
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        is LikedContentDialog.DeletePost -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("이 게시물 삭제") },
                text = { Text("이 게시물을 삭제하시겠습니까?\n삭제 후 복구는 불가능합니다.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deletePost(dialog.postId); activeDialog = null }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { activeDialog = null }) { Text("취소") } }
            )
        }
        is LikedContentDialog.HidePost -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("게시물 숨기기") },
                text = { Text("이 게시물을 숨기시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.hidePost(dialog.postId); activeDialog = null }) {
                        Text("숨기기", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { activeDialog = null }) { Text("취소") } }
            )
        }
        is LikedContentDialog.BlockUser -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("이 사용자 차단") },
                text = { Text("이 사용자를 차단하시겠습니까?\n피드에서 해당 사용자의 모든 글이 즉시 숨겨집니다.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.blockUser(dialog.userId); activeDialog = null }) {
                        Text("차단", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { activeDialog = null }) { Text("취소") } }
            )
        }
        is LikedContentDialog.ReportPost -> {
            ReportDialog(
                onDismiss = { activeDialog = null },
                onConfirm = { reason, detail ->
                    viewModel.reportPost(dialog.postId, reason, detail)
                    activeDialog = null
                }
            )
        }
        is LikedContentDialog.ReportUser -> {
            ReportDialog(
                onDismiss = { activeDialog = null },
                onConfirm = { reason, detail ->
                    viewModel.reportUser(dialog.userId, reason, detail)
                    activeDialog = null
                }
            )
        }
        null -> Unit
    }
}