package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.viewmodel.EditPostViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditPostScreen(
    postId: Long,
    viewModel: EditPostViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    var videoUrl by remember { mutableStateOf<String?>(null) }
    var videoThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isInitialDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(postId) {
        if (!isInitialDataLoaded) {
            val existingPost = viewModel.getPostById(postId)
            if (existingPost != null) {
                title = existingPost.title
                description = existingPost.description
                videoUrl = existingPost.videoUrl
                videoThumbnailUrl = existingPost.videoThumbnailUrl
                imageUrls = existingPost.imageUrls
                isInitialDataLoaded = true
            } else {
                onBack()
            }
        }
    }

    if (!isInitialDataLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Black)
        }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateSuccessEvent.collect {
            onBack()
        }
    }

    val isFormValid = title.isNotBlank() && description.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = "제목") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "내용") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "첨부된 미디어",
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!videoUrl.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "동영상 1개 첨부됨",
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "포스트카드 첫 화면에 고정 노출 중",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        TextButton(
                            onClick = {
                                videoUrl = null
                                videoThumbnailUrl = null
                            }
                        ) {
                            Text("동영상 삭제", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (imageUrls.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "사진 ${imageUrls.size}장 첨부됨",
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "동영상 뒤로 스와이프 노출 중",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        TextButton(onClick = { imageUrls = emptyList() }) {
                            Text("사진 전체 삭제", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    isLoading = true
                    viewModel.editPost(
                        postId = postId,
                        title = title.trim(),
                        description = description.trim(),
                        videoUrl = videoUrl,
                        videoThumbnailUrl = videoThumbnailUrl,
                        imageUrls = imageUrls
                    )
                },
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "게시물 수정",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}