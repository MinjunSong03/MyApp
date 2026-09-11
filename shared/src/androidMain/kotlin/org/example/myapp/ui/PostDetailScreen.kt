package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.myapp.auth.network.CommentResponse
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.auth.viewmodel.CommentUiState
import org.example.myapp.auth.viewmodel.PostDetailViewModel
import org.example.myapp.ui.item.AppTopBar
import org.example.myapp.util.AndroidVideoPlayerManager
import org.example.myapp.util.VideoPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.shared.R
import org.example.myapp.ui.card.CommentCard
import org.example.myapp.ui.dialog.ReportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    onNavigateToEditPost: (Long) -> Unit,
    onNavigateToProfileClick: (Long) -> Unit,
    viewModel: PostDetailViewModel = koinViewModel(),
    videoManager: AndroidVideoPlayerManager = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val mediaHeight = (configuration.screenHeightDp / 4).dp
    val commentSheetHeight = (configuration.screenHeightDp * 0.5f).dp

    val context = LocalContext.current
    var reportingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reportingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reportingCommentId by rememberSaveable { mutableStateOf<Long?>(null) }
    var blockingUserId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hidingPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingCommentId by rememberSaveable { mutableStateOf<Long?>(null) }

    var post by remember { mutableStateOf<PostResponse?>(null) }
    val commentUiState by viewModel.commentUiState.collectAsState()
    val isCommentLoadingMore by viewModel.isCommentLoadingMore.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isInitialDataLoaded by rememberSaveable { mutableStateOf(false) }

    var isCommentSheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val commentListState = rememberLazyListState()

    var commentText by rememberSaveable { mutableStateOf("") }
    var editingComment by remember { mutableStateOf<CommentResponse?>(null) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val shouldLoadMoreComments by remember {
        derivedStateOf {
            val totalItems = commentListState.layoutInfo.totalItemsCount
            val lastVisibleIndex = commentListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMoreComments) {
        if (shouldLoadMoreComments && isCommentSheetOpen) {
            viewModel.loadComments(postId, isRefresh = false)
        }
    }

    LaunchedEffect(isCommentSheetOpen) {
        if (isCommentSheetOpen) {
            viewModel.loadComments(postId, isRefresh = true)
        } else {
            commentText = ""
            editingComment = null
            focusManager.clearFocus()
        }
    }

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
                title = "",
                onBackClick = onBack
            )
        },
        bottomBar = {
            if (post != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 6.dp
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
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "댓글을 입력해 보세요!",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
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
                    val isProfileClickable = !currentPost.isMine && !currentPost.isUserDeleted
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = isProfileClickable) {
                                    onNavigateToProfileClick(currentPost.userId)
                                },
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
                            Column {
                                Text(
                                    text = currentPost.userNickname,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentPost.isUserDeleted) Color.Gray else Color.Black,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentPost.editedAt == null) "조회수 ${currentPost.viewCount}회" else "조회수 ${currentPost.viewCount}회 · 수정됨",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = currentPost.createdAt.substringBefore("T"),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
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
                                                hidingPostId = currentPost.id
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
                                            deletingPostId = currentPost.id
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(text = "이 게시물 숨기기") },
                                        onClick = {
                                            isMenuExpanded = false
                                            hidingPostId = currentPost.id
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
                                            blockingUserId = currentPost.userId
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
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "이 사용자 신고하기",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            isMenuExpanded = false
                                            reportingUserId = currentPost.userId
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

            if (isCommentSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isCommentSheetOpen = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(commentSheetHeight)
                            .navigationBarsPadding()
                            .imePadding()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            }
                    ) {
                        val commentCount = (commentUiState as? CommentUiState.Success)?.comments?.size ?: 0
                        Text(
                            text = "댓글 ${commentCount}개",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        when (val state = commentUiState) {
                            is CommentUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.Black)
                                }
                            }
                            is CommentUiState.Success -> {
                                if (state.comments.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "첫 번째 댓글을 남겨보세요!",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = commentListState,
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                    ) {
                                        items(state.comments, key = { it.id }) { comment ->
                                            CommentCard(
                                                comment = comment,
                                                onProfileClick = onNavigateToProfileClick,
                                                onEditClick = { targetComment ->
                                                    editingComment = targetComment
                                                    commentText = targetComment.content
                                                    focusRequester.requestFocus()
                                                },
                                                onDeleteClick = { commentId ->
                                                    deletingCommentId = commentId
                                                },
                                                onReportCommentClick = { commentId ->
                                                    reportingCommentId = commentId
                                                },
                                                onReportUserClick = { targetUserId ->
                                                    reportingUserId = targetUserId
                                                }
                                            )
                                            HorizontalDivider(
                                                color = Color(0xFFF9F9F9),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }

                                        if (!state.isLast && isCommentLoadingMore) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = Color.Black,
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (editingComment != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0F0F0))
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "댓글 수정 중...",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                TextButton(
                                    onClick = {
                                        editingComment = null
                                        commentText = ""
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Text(
                                        text = "취소",
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                                    decorationBox = { innerTextField ->
                                        if (commentText.isEmpty()) {
                                            Text(
                                                text = "댓글을 입력해 보세요!",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            val targetEdit = editingComment
                                            if (targetEdit != null) {
                                                viewModel.editComment(targetEdit.id, commentText)
                                            } else {
                                                viewModel.createComment(postId, commentText) {
                                                    coroutineScope.launch {
                                                        val currentSize = (commentUiState as? CommentUiState.Success)?.comments?.size ?: 0
                                                        if (currentSize > 0) {
                                                            commentListState.animateScrollToItem(currentSize - 1)
                                                        }
                                                    }
                                                }
                                            }
                                            commentText = ""
                                            editingComment = null
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    },
                                    enabled = commentText.isNotBlank()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_send),
                                        contentDescription = "전송",
                                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 게시물 삭제 확인 다이얼로그
            deletingPostId?.let { targetPostId ->
                AlertDialog(
                    onDismissRequest = { deletingPostId = null },
                    title = { Text(text = "이 게시물 삭제") },
                    text = { Text(text = "이 게시물을 삭제하시겠습니까? 게시물을 삭제 후 복구는 불가능합니다.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePost(targetPostId)
                                deletingPostId = null
                                onBack()
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

            // 게시물 숨기기 확인 다이얼로그
            hidingPostId?.let { targetPostId ->
                AlertDialog(
                    onDismissRequest = { hidingPostId = null },
                    title = { Text(text = "게시물 숨기기") },
                    text = { Text(text = "이 게시물을 숨기시겠습니까?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.hidePost(targetPostId)
                                hidingPostId = null
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
                        TextButton(onClick = { hidingPostId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            // 사용자 차단 확인 다이얼로그
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
                        TextButton(onClick = { blockingUserId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            // 댓글 삭제 확인 다이얼로그
            deletingCommentId?.let { targetCommentId ->
                AlertDialog(
                    onDismissRequest = { deletingCommentId = null },
                    title = { Text(text = "댓글 삭제") },
                    text = { Text(text = "이 댓글을 삭제하시겠습니까? 삭제 후 복구는 불가능합니다.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteComment(targetCommentId)
                                deletingCommentId = null
                            }
                        ) {
                            Text(text = "삭제", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingCommentId = null }) {
                            Text(text = "취소")
                        }
                    }
                )
            }

            // 게시물 신고 다이얼로그
            reportingPostId?.let { targetPostId ->
                ReportDialog(
                    onDismiss = { reportingPostId = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportPost(targetPostId, reportReason, detail)
                        reportingPostId = null
                    }
                )
            }

            // 사용자 신고 다이얼로그
            reportingUserId?.let { targetUserId ->
                ReportDialog(
                    onDismiss = { reportingUserId = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportUser(targetUserId, reportReason, detail)
                        reportingUserId = null
                    }
                )
            }

            // 댓글 신고 다이얼로그
            reportingCommentId?.let { targetCommentId ->
                ReportDialog(
                    onDismiss = { reportingCommentId = null },
                    onConfirm = { reportReason, detail ->
                        viewModel.reportComment(targetCommentId, reportReason, detail)
                        reportingCommentId = null
                    }
                )
            }
        }
    }
}