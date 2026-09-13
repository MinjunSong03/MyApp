package org.example.myapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 라이트 모드 팔레트
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF191919),           // 주요 버튼/강조 배경 (블랙)
    onPrimary = Color.White,               // 버튼 위 텍스트 (화이트)
    background = Color(0xFFF8F9FA),        // 앱 전체 화면 배경
    onBackground = Color(0xFF191919),      // 화면 배경 위 기본 텍스트
    surface = Color.White,                 // 카드, 바텀시트, 다이얼로그, 탑바 배경
    onSurface = Color(0xFF191919),         // 표면 위 메인 텍스트 및 기본 아이콘
    surfaceVariant = Color(0xFFF1F3F5),    // 미디어 플레이어 배경, 칩/서브박스 배경
    onSurfaceVariant = Color(0xFF757575),  // 부가 정보(날짜, 조회수, 서브텍스트)
    outline = Color(0xFFCCCCCC),           // 텍스트필드 보더 등
    outlineVariant = Color(0xFFEEEEEE),    // 리스트 구분선, 카드 경계선
    error = Color(0xFFFF5252),             // 삭제, 신고, 탈퇴 액션
    onError = Color.White
)

// 다크 모드 팔레트
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF1F3F5),           // 주요 버튼/강조 배경 (화이트)
    onPrimary = Color(0xFF121212),         // 버튼 위 텍스트 (블랙)
    background = Color(0xFF121212),        // 앱 전체 화면 배경 (다크그레이/블랙)
    onBackground = Color(0xFFF1F3F5),      // 화면 배경 위 기본 텍스트
    surface = Color(0xFF1E1E1E),           // 카드, 바텀시트, 다이얼로그, 탑바 배경
    onSurface = Color(0xFFF1F3F5),         // 표면 위 메인 텍스트 및 기본 아이콘
    surfaceVariant = Color(0xFF2C2C2C),    // 미디어 플레이어 배경, 칩/서브박스 배경
    onSurfaceVariant = Color(0xFF9E9E9E),  // 부가 정보(날짜, 조회수, 서브텍스트)
    outline = Color(0xFF666666),           // 텍스트필드 보더 등
    outlineVariant = Color(0xFF2E2E2E),    // 리스트 구분선, 카드 경계선
    error = Color(0xFFFF6B6B),             // 다크모드용 살짝 밝은 레드
    onError = Color.Black
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}