package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.viewmodel.PostDetailViewModel
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.util.AndroidVideoPlayerManager
import org.example.myapp.util.VideoPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.shared.R
import org.example.myapp.ui.dialog.ReportDialog

@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    viewModel: PostDetailViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val mediaHeight = (configuration.screenHeightDp / 3).dp

    val context = LocalContext.current
    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }

    var post by remember { mutableStateOf<PostResponse?>(null) }
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

    var isInitialDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoManager.pause()
        }
    }

    LaunchedEffect(postId) {
        if (!isInitialDataLoaded) {
            val existingPost = viewModel.getPostDetail(postId)
            if (existingPost != null) {
                post = existingPost
            } else {
                onBack()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppTopBar(
                title = "상세 보기",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        val currentPost = post
        if (currentPost == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Black)
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
                    .background(Color.White)
            ) {
                if (mediaItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mediaHeight)
                            .background(Color(0xFFF0F0F0)),
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
                        AsyncImage(
                            model = currentPost.userProfileImageUrl,
                            contentDescription = "프로필 사진",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentPost.userNickname,
                                fontWeight = FontWeight.Bold,
                                color = if (currentPost.isUserDeleted) Color.Gray else Color.Black,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "조회수 ${currentPost.viewCount}회",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_option),
                                    contentDescription = "옵션",
                                    tint = Color.Gray
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                containerColor = Color.White
                            ) {
                                if (currentPost.isMine) {
                                    if (currentPost.isHidden) {
                                        DropdownMenuItem(
                                            text = { Text(text = "내 게시물 숨기기 해제") },
                                            onClick = {
                                                isMenuExpanded = false
                                                viewModel.unhidePost(currentPost)
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text(text = "내 게시물 숨기기") },
                                            onClick = {
                                                isMenuExpanded = false
                                                viewModel.hidePost(currentPost.id)
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
                                            viewModel.deletePost(currentPost.id)
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(text = "이 게시물 숨기기") },
                                        onClick = {
                                            isMenuExpanded = false
                                            viewModel.hidePost(currentPost.id)
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
                                            viewModel.blockUser(currentPost.userId)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(
                                            text = "이 게시물 신고하기",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = {
                                            isMenuExpanded = false
                                            reportingPostId = currentPost.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = currentPost.title,
                        maxLines = 3,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentPost.description,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF333333)
                    )
                }
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
}