package org.example.myapp.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.viewmodel.CreatePostViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.example.myapp.util.toPickedMedia


@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedVideo by remember { mutableStateOf<PickedMedia?>(null) }
    var selectedImages by remember { mutableStateOf<List<PickedMedia>>(emptyList()) }

    val isLoading by viewModel.isLoading.collectAsState()
    val isFormValid = title.isNotBlank() && description.isNotBlank()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val media = uri.toPickedMedia(context)
            if (media != null && media.mediaType == MediaType.VIDEO) {
                selectedVideo = media
            } else if (media != null) {
                Toast.makeText(context, "동영상 파일만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "파일을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val images = uris.mapNotNull { it.toPickedMedia(context) }
                .filter { it.mediaType == MediaType.IMAGE }
            if (images.isNotEmpty()) {
                selectedImages = images.take(10)
            } else {
                Toast.makeText(context, "사진 파일만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
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
            label = { Text(text = "제목(필수)") },
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
            label = { Text(text = "내용(필수)") },
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
            text = "미디어 첨부 (동영상 최대 1개, 사진 최대 10장)",
            fontSize = 14.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (selectedVideo == null) "동영상 추가" else "동영상 변경")
            }

            Button(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (selectedImages.isEmpty()) "사진 추가" else "사진 다시 선택")
            }
        }
        if (selectedVideo != null) {
            Spacer(modifier = Modifier.height(10.dp))
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
                            text = "동영상: ${selectedVideo?.fileName}",
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "동영상",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = { selectedVideo = null }) {
                        Text("삭제", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
        if (selectedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
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
                            text = "사진 ${selectedImages.size}장 선택됨",
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "사진",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = { selectedImages = emptyList() }) {
                        Text("전체 삭제", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                viewModel.createPost(
                    title = title.trim(),
                    description = description.trim(),
                    video = selectedVideo,
                    images = selectedImages
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
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "게시물 생성",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}