package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.viewmodel.ProfileClickViewModel
import org.example.myapp.auth.viewmodel.MyPostUiState
import org.example.myapp.auth.viewmodel.ProfileClickUiState
import org.example.myapp.shared.R
import org.example.myapp.ui.card.PostCard
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.util.AndroidVideoPlayerManager
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileClickScreen(
    userId: Long,
    onBack: () -> Unit,
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToEditPost: (Long) -> Unit = {},
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    viewModel: ProfileClickViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var blockingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hidingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var unhidingPost by rememberSaveable { mutableStateOf<PostResponse?>(null) }

    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
        viewModel.loadPost(userId, isRefresh = true)
    }

    LaunchedEffect(listState, uiState) {
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
                        val postsCount = (uiState as? ProfileClickUiState.Success)?.posts?.size ?: 0
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
            val state = uiState
            if (centerIndex != null && state is ProfileClickUiState.Success) {
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
            viewModel.loadPost(userId, false)
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
                title = "프로필 보기",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProfileClickUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                }
                is ProfileClickUiState.Success -> {
                    userProfile?.let { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "프로필 사진",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.nickname,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isDeleted) Color.Gray else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                            if (!user.isMine) {
                                Box {
                                    IconButton(onClick = { isMenuExpanded = true }) {
                                        Icon(
                                            painterResource(R.drawable.ic_option),
                                            contentDescription = "옵션",
                                            tint = Color.Gray
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = isMenuExpanded,
                                        onDismissRequest = { isMenuExpanded = false },
                                        containerColor = Color.White
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "이 사용자 차단하기",
                                                )
                                            },
                                            onClick = {
                                                isMenuExpanded = false
                                                blockingUserId = user.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                videoManager.stop()
                                viewModel.loadPost(userId = userId, isRefresh = true)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (state.posts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "활성화 게시물이 없습니다.",
                                        color = Color.Gray
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
                                            videoManager = videoManager,
                                            onCardClick = onNavigateToPostDetail,
                                            onProfileClick = {},
                                            onEditClick = onNavigateToEditPost,
                                            onDeleteClick = { deletingPostId = it },
                                            onUnhidePostClick = { unhidingPost = post },
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