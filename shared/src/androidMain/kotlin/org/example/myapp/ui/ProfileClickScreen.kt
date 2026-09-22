package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.viewmodel.ProfileClickViewModel
import org.example.myapp.auth.viewmodel.ProfileClickUiState
import org.example.myapp.shared.R
import org.example.myapp.ui.card.PostCard
import org.example.myapp.ui.dialog.ReportDialog
import org.example.myapp.ui.item.AppPullToRefreshBox
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.ui.item.PostFeedList
import org.example.myapp.util.AndroidVideoPlayerManager
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private sealed interface ProfileClickDialog {
    data class DeletePost(val postId: Long): ProfileClickDialog
    data class HidePost(val postId: Long): ProfileClickDialog
    data class BlockUser(val userId: Long): ProfileClickDialog
    data class ReportPost(val postId: Long): ProfileClickDialog
    data class ReportUser(val userId: Long): ProfileClickDialog
}

@Composable
fun ProfileClickScreen(
    userId: Long,
    onBack: () -> Unit,
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    viewModel: ProfileClickViewModel = koinViewModel(key = userId.toString()) { parametersOf(userId) },
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var activeDialog by remember { mutableStateOf<ProfileClickDialog?>(null) }
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }


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
                title = "프로필 보기",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val profile = uiState.userProfileResponse
            val isLiked = profile?.isLiked == true
            val likeCount = profile?.likeCount ?: 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profile?.profileImageUrl,
                        contentDescription = "프로필 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = profile?.nickname ?: "",
                        fontWeight = FontWeight.Bold,
                        color = if (profile?.isDeleted == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleLikeUser() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like
                            ),
                            contentDescription = if (isLiked) "좋아요 취소" else "좋아요",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (likeCount > 0) {
                        Text(
                            text = "$likeCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                if (profile?.isMine == false) {
                    Box {
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
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
                            DropdownMenuItem(
                                text = { Text(text = "이 사용자 차단하기") },
                                onClick = {
                                    isMenuExpanded = false
                                    activeDialog = ProfileClickDialog.BlockUser(userId)
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
                                    activeDialog = ProfileClickDialog.ReportUser(userId)
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PostFeedList(
                posts = uiState.posts,
                isInitialLoading = uiState.isInitialLoading,
                isRefreshing = uiState.isRefreshing,
                isLast = uiState.isLast,
                emptyMessage = "게시물이 없습니다.",
                videoManager = videoManager,
                listState = listState,
                onRefresh = { viewModel.loadPost(isRefresh = true) },
                onLoadMore = { viewModel.loadPost(isRefresh = false) },
                onCardClick = onNavigateToPostDetail,
                onProfileClick = { },
                onLikeClick = { viewModel.toggleLikePost(it)},
                onEditClick = onNavigateToEditPost,
                onDeleteClick = { activeDialog = ProfileClickDialog.DeletePost(it) },
                onHideClick = { activeDialog = ProfileClickDialog.HidePost(it) },
                onBlockClick = { activeDialog = ProfileClickDialog.BlockUser(it) },
                onReportPostClick = { activeDialog = ProfileClickDialog.ReportPost(it) },
                onReportUserClick = { activeDialog = ProfileClickDialog.ReportUser(it) }
            )
        }
        when (val dialog = activeDialog) {
            is ProfileClickDialog.DeletePost -> {
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

            is ProfileClickDialog.HidePost -> {
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

            is ProfileClickDialog.BlockUser -> {
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

            is ProfileClickDialog.ReportPost -> {
                ReportDialog(
                    onDismiss = { activeDialog = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportPost(dialog.postId, reportReason, detail)
                        activeDialog = null
                    }
                )
            }

            is ProfileClickDialog.ReportUser -> {
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