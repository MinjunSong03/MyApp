package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.example.myapp.auth.viewmodel.HomeUiState
import org.example.myapp.auth.viewmodel.HomeViewModel
import org.example.myapp.shared.R
import org.example.myapp.ui.card.PostCard
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.util.AndroidVideoPlayerManager


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToEditPost: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var blockingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hidingPostId by rememberSaveable { mutableStateOf<Long?>(null) }

    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

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

    LaunchedEffect(listState, uiState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) null
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }?.index
            }
        }.collect { centerIndex ->
            if (centerIndex != null && uiState is HomeUiState.Success) {
                val posts = (uiState as HomeUiState.Success).posts
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                videoManager.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoManager.pause()
        }
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
        viewModel.loadHomeFeed(isRefresh = true)
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
        .nestedScroll(nestedScrollConnection)
    ) {
        PullToRefreshBox(
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
                        color = Color.Black,
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
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                        ) {
                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    videoManager = videoManager,
                                    onEditClick = { onNavigateToEditPost(post.id) },
                                    onDeleteClick = { deletingPostId = it },
                                    onUnhidePostClick = {},
                                    onHidePostClick = { hidingPostId = it },
                                    onBlockUserClick = { blockingUserId = it },
                                    onReportPostClick = { reportingPostId = it }
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

        AnimatedVisibility(
            visible = isFloatingVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 110.dp)
        ) {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                containerColor = Color.Black,
                shape = CircleShape,
                contentColor = Color.White,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = "글 작성"
                )
            }
        }

        deletingPostId?.let { postId ->
            AlertDialog(
                onDismissRequest = { deletingPostId = null },
                title = { Text(text = "이 게시물 삭제") },
                text = { Text(text = "이 게시물을 삭제하시겠습니까? 게시물을 삭제 후 복구는 불가능합니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePost(postId)
                            deletingPostId = null
                        }
                    ) {
                        Text(text = "삭제", color = MaterialTheme.colorScheme.error)
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
                text = { Text(text = "이 사용자를 차단하시겠습니까? 피드에서 해당 사용자의 모든 글이 즉시 숨겨집니다.") },
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
    }
}