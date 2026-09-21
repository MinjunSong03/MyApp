package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.viewmodel.PostDetailViewModel
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.util.AndroidVideoPlayerManager
import org.example.myapp.util.VideoPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.shared.R
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.CommentBottomSheet
import org.koin.core.parameter.parametersOf

private sealed interface PostDetailDialog {
    data class DeletePost(val postId: Long): PostDetailDialog
    data class DeleteComment(val commentId: Long): PostDetailDialog
    data class HidePost(val postId: Long): PostDetailDialog
    data class UnhidePost(val postId: Long) : PostDetailDialog
    data class BlockUser(val userId: Long): PostDetailDialog
    data class ReportPost(val postId: Long): PostDetailDialog
    data class ReportComment(val commentId: Long): PostDetailDialog
    data class ReportUser(val userId: Long): PostDetailDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    onNavigateToProfileClick: (Long) -> Unit,
    viewModel: PostDetailViewModel = koinViewModel(key = postId.toString()) { parametersOf(postId) },
    videoManager: AndroidVideoPlayerManager = koinViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val mediaHeight = (configuration.screenHeightDp / 4).dp

    var activeDialog by remember { mutableStateOf<PostDetailDialog?>(null) }

    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isCommentSheetOpen by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commentUiState by viewModel.commentUiState.collectAsStateWithLifecycle()

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

    LaunchedEffect(isCommentSheetOpen) {
        if (isCommentSheetOpen) {
            viewModel.loadComments(isRefresh = true)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                onBackClick = onBack
            )
        },
        bottomBar = {
            if (uiState.post != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .clickable { isCommentSheetOpen = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "댓글을 입력해 보세요!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val currentPost = uiState.post
        if (currentPost == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val mediaItems = currentPost.mediaItems
            val pagerState = rememberPagerState(pageCount = { mediaItems.size })

            LaunchedEffect(pagerState.currentPage, currentPost) {
                val item = mediaItems.getOrNull(pagerState.currentPage)
                if (item?.mediaType == MediaType.VIDEO) {
                    videoManager.play(item.mediaUrl)
                } else {
                    videoManager.pause()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (mediaItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mediaHeight)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val item = mediaItems[page]
                            when (item.mediaType) {
                                MediaType.VIDEO -> {
                                    VideoPlayer(
                                        videoUrl = item.mediaUrl,
                                        thumbnailUrl = item.thumbnailUrl,
                                        videoManager = videoManager,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                MediaType.IMAGE -> {
                                    AsyncImage(
                                        item.mediaUrl,
                                        currentPost.title,
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        if (mediaItems.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { onNavigateToProfileClick(currentPost.userId) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentPost.userProfileImageUrl,
                                contentDescription = "프로필 사진",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentPost.userNickname,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPost.isUserDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_option),
                                    contentDescription = "옵션",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (currentPost.isMine) {
                                    if (currentPost.isHidden) {
                                        DropdownMenuItem(
                                            text = { Text(text = "내 게시물 숨기기 해제") },
                                            onClick = {
                                                isMenuExpanded = false
                                                viewModel.unhidePost(currentPost.id)
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text(text = "내 게시물 숨기기") },
                                            onClick = {
                                                isMenuExpanded = false
                                                activeDialog = PostDetailDialog.HidePost(currentPost.id)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(text = "내 게시물 수정하기") },
                                        onClick = {
                                            isMenuExpanded = false
                                            onNavigateToEditPost(currentPost.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(
                                            text = "내 게시물 삭제하기",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = {
                                            isMenuExpanded = false
                                            activeDialog = PostDetailDialog.DeletePost(currentPost.id)
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(text = "이 게시물 숨기기") },
                                        onClick = {
                                            isMenuExpanded = false
                                            activeDialog = PostDetailDialog.HidePost(currentPost.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "이 사용자 차단하기",
                                            )
                                        },
                                        onClick = {
                                            isMenuExpanded = false
                                            activeDialog = PostDetailDialog.BlockUser(currentPost.userId)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(
                                            text = "이 게시물 신고하기",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = {
                                            isMenuExpanded = false
                                            activeDialog = PostDetailDialog.ReportPost(currentPost.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "이 사용자 신고하기",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            isMenuExpanded = false
                                            activeDialog = PostDetailDialog.ReportUser(currentPost.userId)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentPost.editedAt == null) "조회수 ${currentPost.viewCount}회" else "조회수 ${currentPost.viewCount}회 · 수정됨",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentPost.createdAt.substringBefore("T"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = currentPost.title,
                        maxLines = 3,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentPost.description,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isCommentSheetOpen) {
                CommentBottomSheet(
                    commentUiState = commentUiState,
                    isRefreshing = commentUiState.isRefreshing,
                    onRefresh = { viewModel.loadComments(isRefresh = true)},
                    onDismissRequest = { isCommentSheetOpen = false },
                    onLoadMore = { viewModel.loadComments(isRefresh = false) },
                    onCommentTextChanged = { viewModel.onCommentTextChanged(it) },
                    onStartEditComment = { viewModel.startEditComment(it) },
                    onCancelEditComment = { viewModel.cancelEditComment() },
                    onCreateComment = { viewModel.createComment(postId, it) },
                    onEditComment = { id, text -> viewModel.editComment(id, text) },
                    onDeleteClick = { activeDialog = PostDetailDialog.DeleteComment(it) },
                    onReportCommentClick = { activeDialog = PostDetailDialog.ReportComment(it) },
                    onReportUserClick = { activeDialog = PostDetailDialog.ReportUser(it) },
                    onProfileClick = onNavigateToProfileClick
                )
            }

            when (val dialog = activeDialog) {
                is PostDetailDialog.DeletePost -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "이 게시물 삭제") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = { Text(text = "이 게시물을 삭제하시겠습니까?\n게시물을 삭제 후 복구는 불가능합니다.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePost(dialog.postId)
                                    activeDialog = null
                                    onBack()
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

                is PostDetailDialog.DeleteComment -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "댓글 삭제") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = { Text(text = "이 댓글을 삭제하시겠습니까?\n삭제 후 복구는 불가능합니다.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteComment(dialog.commentId)
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

                is PostDetailDialog.HidePost -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "게시물 숨기기") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = { Text(text = "이 게시물을 숨기시겠습니까?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.hidePost(dialog.postId)
                                    activeDialog = null
                                    onBack()
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

                is PostDetailDialog.UnhidePost -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text("게시물 숨기기 해제") },
                        text = { Text("이 게시물의 숨김 처리를 해제하시겠습니까?") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.unhidePost(dialog.postId); activeDialog = null }) {
                                Text("해제")
                            }
                        },
                        dismissButton = { TextButton(onClick = { activeDialog = null }) { Text("취소") } }
                    )
                }

                is PostDetailDialog.BlockUser -> {
                    AlertDialog(
                        onDismissRequest = { activeDialog = null },
                        title = { Text(text = "이 사용자 차단") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        text = { Text(text = "이 사용자를 차단하시겠습니까?\n피드에서 해당 사용자의 모든 글이 즉시 숨겨집니다.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.blockUser(dialog.userId)
                                    activeDialog = null
                                    onBack()
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

                is PostDetailDialog.ReportPost -> {
                    ReportDialog(
                        onDismiss = { activeDialog = null },
                        onConfirm = { reportReason, detail ->
                            viewModel.reportPost(dialog.postId, reportReason, detail)
                            activeDialog = null
                        }
                    )
                }

                is PostDetailDialog.ReportComment -> {
                    ReportDialog(
                        onDismiss = { activeDialog = null },
                        onConfirm = { reportReason, detail ->
                            viewModel.reportComment(dialog.commentId, reportReason, detail)
                            activeDialog = null
                        }
                    )
                }

                is PostDetailDialog.ReportUser -> {
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