package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.myapp.auth.viewmodel.MyPostViewModel
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.ui.item.PostFeedList
import org.example.myapp.util.AndroidVideoPlayerManager
import org.koin.compose.viewmodel.koinViewModel

private sealed interface MyPostDialog {
    data class DeletePost(val postId: Long) : MyPostDialog
    data class HidePost(val postId: Long) : MyPostDialog
    data class UnhidePost(val postId: Long) : MyPostDialog
}

@Composable
fun MyPostScreen(
    viewModel: MyPostViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel(),
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    onNavigateToProfileClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val actFeed by viewModel.actFeed.collectAsStateWithLifecycle()
    val hiddenFeed by viewModel.hiddenFeed.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var activeDialog by remember { mutableStateOf<MyPostDialog?>(null) }

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
                title = "나의 게시물",
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
                    text = { Text("활성화 게시물") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("숨긴 게시물") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    PostFeedList(
                        posts = actFeed.posts,
                        isInitialLoading = actFeed.isInitialLoading,
                        isRefreshing = actFeed.isRefreshing,
                        isLast = actFeed.isLast,
                        emptyMessage = "활성화된 게시물이 없습니다.",
                        videoManager = videoManager,
                        onRefresh = { viewModel.loadActFeed(isRefresh = true) },
                        onLoadMore = { viewModel.loadActFeed(isRefresh = false) },
                        onCardClick = onNavigateToPostDetail,
                        onProfileClick = onNavigateToProfileClick,
                        onLikeClick = { viewModel.toggleLikePost(it) },
                        onEditClick = onNavigateToEditPost,
                        onDeleteClick = { activeDialog = MyPostDialog.DeletePost(it) },
                        onHideClick = { activeDialog = MyPostDialog.HidePost(it) }
                    )
                } else {
                    PostFeedList(
                        posts = hiddenFeed.posts,
                        isInitialLoading = hiddenFeed.isInitialLoading,
                        isRefreshing = hiddenFeed.isRefreshing,
                        isLast = hiddenFeed.isLast,
                        emptyMessage = "숨긴 게시물이 없습니다.",
                        videoManager = videoManager,
                        onRefresh = { viewModel.loadHiddenFeed(isRefresh = true) },
                        onLoadMore = { viewModel.loadHiddenFeed(isRefresh = false) },
                        onCardClick = onNavigateToPostDetail,
                        onProfileClick = onNavigateToProfileClick,
                        onLikeClick = { viewModel.toggleLikePost(it) },
                        onEditClick = onNavigateToEditPost,
                        onDeleteClick = { activeDialog = MyPostDialog.DeletePost(it) },
                        onHideClick = { },
                        onUnhideClick = { activeDialog = MyPostDialog.UnhidePost(it) }
                    )
                }
            }
        }
    }

    when (val dialog = activeDialog) {
        is MyPostDialog.DeletePost -> {
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
        is MyPostDialog.HidePost -> {
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
        is MyPostDialog.UnhidePost -> {
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
        null -> Unit
    }
}