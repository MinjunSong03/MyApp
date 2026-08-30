package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.viewmodel.MyPostUiState
import org.example.myapp.auth.viewmodel.MyPostViewModel
import org.example.myapp.auth.viewmodel.PostTab
import org.example.myapp.shared.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyPostScreen(
    viewModel: MyPostViewModel = koinViewModel(),
    onNavigateToEditPost: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var blockingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hidingPostId by rememberSaveable { mutableStateOf<Long?>(null) }

    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(currentTab) {
        viewModel.loadMyPost(isRefresh = true)
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMyPost(isRefresh = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
            Text(
                text = "나의 게시물",
                fontSize = 18.sp,
                color = Color.Black
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.switchTab(PostTab.Act) },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTab is PostTab.Act) Color.DarkGray else Color.LightGray)
            ) {
                Text(
                    text = "활성화 게시물",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.padding(5.dp))
            Button(
                onClick = { viewModel.switchTab(PostTab.Hidden) },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentTab is PostTab.Hidden) Color.DarkGray else Color.LightGray)
            ) {
                Text(
                    text = "숨긴 게시물",
                    color = Color.White
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadMyPost(isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is MyPostUiState.Loading -> {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MyPostUiState.Success -> {
                        if (state.posts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentTab is PostTab.Act) "활성화 게시물이 없습니다." else "숨긴 게시물이 없습니다.",
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    PostCard(
                                        post = post,
                                        onEditClick = { onNavigateToEditPost(post.id) },
                                        onDeleteClick = { deletingPostId = it },
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