package org.example.myapp.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.CommentResponse
import org.example.myapp.shared.R

@Composable
fun CommentCard(
    comment: CommentResponse,
    onProfileClick: (Long) -> Unit,
    onEditClick: (CommentResponse) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onReportCommentClick: (Long) -> Unit,
    onReportUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val isProfileClickable = !comment.isMine && !comment.isUserDeleted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.userProfileImageUrl,
            contentDescription = "프로필 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable(enabled = isProfileClickable) {
                    onProfileClick(comment.userId)
                }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.userNickname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (comment.isUserDeleted) Color.Gray else Color.Black,
                    modifier = Modifier.clickable(enabled = isProfileClickable) {
                        onProfileClick(comment.userId)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (comment.editedAt == null) {
                        comment.createdAt.substringBefore("T")
                    } else {
                        "${comment.createdAt.substringBefore("T")} · 수정됨"
                    },
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF222222)
            )
        }

        Box {
            IconButton(
                onClick = { isMenuExpanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_option),
                    contentDescription = "댓글 옵션",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                containerColor = Color.White
            ) {
                if (comment.isMine) {
                    DropdownMenuItem(
                        text = { Text("댓글 수정하기") },
                        onClick = {
                            isMenuExpanded = false
                            onEditClick(comment)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("댓글 삭제하기", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onDeleteClick(comment.id)
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Text("댓글 신고하기", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onReportCommentClick(comment.id)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("작성자 신고하기", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onReportUserClick(comment.userId)
                        }
                    )
                }
            }
        }
    }
}