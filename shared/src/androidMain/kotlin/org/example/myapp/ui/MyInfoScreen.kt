package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import org.example.myapp.auth.model.AuthState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.viewmodel.MyInfoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyInfoScreen(
    viewModel: MyInfoViewModel = koinViewModel(),
    onUpdateNicknameClick: () -> Unit,
    onMyPostClick: () -> Unit,
    onManageMyClick: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var withdrawClick by rememberSaveable { mutableStateOf(false) }
    var withdrawRecheckClick by rememberSaveable { mutableStateOf(false) }

    val session = when (val state = authState) {
        is AuthState.Authenticated -> state.session
        else -> null
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (withdrawClick) {
        AlertDialog(
            onDismissRequest = { withdrawClick = false },
            title = {
                Text(text = "회원탈퇴")
            },
            text = {
                Text(text = "2단계 중 1단계\n\n정말 회원탈퇴하시겠습니까?\n\n탈퇴 후 작성하신 게시물은 자동 삭제되지 않습니다. '나의 게시물' 탭에서 나의 모든 게시물을 삭제할 수 있습니다.\n\n위 내용을 이해하였으며, 회원탈퇴를 진행하시겠습니까?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        withdrawClick = false
                        withdrawRecheckClick = true
                    }
                ) {
                    Text(text = "탈퇴", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { withdrawClick = false }
                ) {
                    Text(text = "취소", color = Color.Gray)
                }
            }
        )
    }

    if (withdrawRecheckClick) {
        AlertDialog(
            onDismissRequest = { withdrawRecheckClick = false },
            title = {
                Text(text = "회원탈퇴")
            },
            text = {
                Text(text = "2단계 중 2단계\n\n정말 회원탈퇴를 진행하시겠습니까?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        withdrawRecheckClick = false
                        viewModel.unlink(OAuthProvider.KAKAO)
                    }
                ) {
                    Text(text = "탈퇴", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { withdrawRecheckClick = false }
                ) {
                    Text(text = "취소", color = Color.Gray)
                }
            }
        )
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${session?.nickname} 님.",
                color = Color.Black,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "반갑습니다!",
                color = Color.Black,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "고유번호: ${session?.userId ?: "-"}",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onUpdateNicknameClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "닉네임 변경",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onMyPostClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "나의 게시물",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onManageMyClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "차단한 사용자 관리",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.logout(OAuthProvider.KAKAO) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text(
                    text = "로그아웃",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { withdrawClick = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text(
                    text = "회원탈퇴",
                    color = Color.White
                )
            }
        }
    }
}