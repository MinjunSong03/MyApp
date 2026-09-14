package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import org.example.myapp.auth.model.AuthState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.viewmodel.MyInfoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyInfoScreen(
    viewModel: MyInfoViewModel = koinViewModel(),
    onUpdateNicknameClick: () -> Unit,
    onMyPostClick: () -> Unit,
    onManageMyClick: () -> Unit,
    onLicenseClick: () -> Unit
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
            containerColor = MaterialTheme.colorScheme.surface,
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
                    Text(
                        text = "탈퇴",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { withdrawClick = false }
                ) {
                    Text(
                        text = "취소",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    if (withdrawRecheckClick) {
        AlertDialog(
            onDismissRequest = { withdrawRecheckClick = false },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    Text(
                        text = "탈퇴",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { withdrawRecheckClick = false }
                ) {
                    Text(
                        text = "취소",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${session?.nickname} 님.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "반갑습니다!",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "고유번호: ${session?.userId ?: "-"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onUpdateNicknameClick() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "프로필 수정"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onMyPostClick() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "나의 게시물"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onManageMyClick() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "차단한 사용자 관리"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.logout(OAuthProvider.KAKAO) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "로그아웃"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { withdrawClick = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "회원탈퇴"
                )
            }
        }
        Text(
            text = "오픈소스 라이선스",
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clickable { onLicenseClick() }
        )
    }
}