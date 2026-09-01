package org.example.myapp.ui.card

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.network.PostResponse
import org.example.myapp.shared.R
import org.example.myapp.util.VideoPlayer


@Composable
fun PostCard(
    post: PostResponse,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onUnhidePostClick: (Long) -> Unit = {},
    onHidePostClick: (Long) -> Unit,
    onBlockUserClick: (Long) -> Unit,
    onReportPostClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.userProfileImageUrl,
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userNickname,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isUserDeleted) Color.Gray else Color.Black,
                        fontSize = 14.sp
                    )
                }

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
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        if (post.isMine) {
                            if (post.isHidden) {
                                DropdownMenuItem(
                                    text = { Text(text = "내 게시물 숨기기 해제") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onUnhidePostClick(post.id)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(text = "내 게시물 숨기기") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onHidePostClick(post.id)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = "내 게시물 수정하기") },
                                onClick = {
                                    isMenuExpanded = false
                                    onEditClick(post.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(
                                    text = "내 게시물 삭제하기",
                                    color = MaterialTheme.colorScheme.error
                                ) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDeleteClick(post.id)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(text = "이 게시물 숨기기") },
                                onClick = {
                                    isMenuExpanded = false
                                    onHidePostClick(post.id)
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
                                    onBlockUserClick(post.userId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(
                                    text = "이 게시물 신고하기",
                                    color = MaterialTheme.colorScheme.error
                                ) },
                                onClick = {
                                    isMenuExpanded = false
                                    onReportPostClick(post.id)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                when (post.mediaType) {
                    MediaType.IMAGE -> {
                        AsyncImage(
                            model = post.thumbnailUrl,
                            contentDescription = post.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.VIDEO -> {
                        VideoPlayer(
                            videoUrl = post.mediaUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.description,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "조회수 ${post.viewCount}회",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}