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
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reportingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var blockingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hidingPostId by rememberSaveable { mutableStateOf<Long?>(null) }

    val currentUiState by rememberUpdatedState(uiState)

    var isFloatingVisible by rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) {
                    isFloatingVisible = false
                } else if (available.y > 10f) {
                    isFloatingVisible = true
                }
                return Offset.Zero
            }
        }
    }

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
                        val postsCount = (uiState as? HomeUiState.Success)?.posts?.size ?: 0
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
            if (centerIndex != null && state is HomeUiState.Success) {
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

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        videoManager.pause()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        videoManager.pause()
    }

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
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            AppPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    videoManager.stop()
                    viewModel.loadHomeFeed(isRefresh = true)
                            },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is HomeUiState.Success -> {
                        if (state.posts.isEmpty()) {
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
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostCard(
                                        post = post,
                                        onCardClick = onNavigateToPostDetail,
                                        onProfileClick = onNavigateToProfileClick,
                                        videoManager = videoManager,
                                        onEditClick = onNavigateToEditPost,
                                        onDeleteClick = { deletingPostId = it },
                                        onUnhidePostClick = {},
                                        onHidePostClick = { hidingPostId = it },
                                        onBlockUserClick = { blockingUserId = it },
                                        onReportPostClick = { reportingPostId = it },
                                        onReportUserClick = { reportingUserId = it }
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
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            deletingPostId?.let { postId ->
                AlertDialog(
                    onDismissRequest = { deletingPostId = null },
                    title = { Text(text = "이 게시물 삭제") },
                    text = { Text(text = "이 게시물을 삭제하시겠습니까?\n게시물을 삭제 후 복구는 불가능합니다.") },
                    containerColor = MaterialTheme.colorScheme.surface,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePost(postId)
                                deletingPostId = null
                            }
                        ) {
                            Text(
                                text = "삭제",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingPostId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            hidingPostId?.let { postId ->
                AlertDialog(
                    onDismissRequest = { hidingPostId = null },
                    title = { Text(text = "게시물 숨기기") },
                    text = { Text(text = "이 게시물을 숨기시겠습니까?") },
                    containerColor = MaterialTheme.colorScheme.surface,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.hidePost(postId)
                                hidingPostId = null
                            }
                        ) {
                            Text(
                                text = "숨기기",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { hidingPostId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            blockingUserId?.let { targetId ->
                AlertDialog(
                    onDismissRequest = { blockingUserId = null },
                    title = { Text(text = "이 사용자 차단") },
                    text = { Text(text = "이 사용자를 차단하시겠습니까?\n피드에서 해당 사용자의 모든 글이 즉시 숨겨집니다.") },
                    containerColor = MaterialTheme.colorScheme.surface,
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.blockUser(targetId)
                                blockingUserId = null
                            }
                        ) {
                            Text(
                                text = "차단",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { blockingUserId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            reportingPostId?.let { postId ->
                ReportDialog(
                    onDismiss = { reportingPostId = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportPost(postId, reportReason, detail)
                        reportingPostId = null
                    }
                )
            }

            reportingUserId?.let { userId ->
                ReportDialog(
                    onDismiss = { reportingUserId = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportUser(userId, reportReason, detail)
                        reportingUserId = null
                    }
                )
            }
        }
    }
}