package org.example.myapp.auth.platform

import android.content.Context
import com.kakao.sdk.common.KakaoSdk

fun initKakaoSdk(context: Context, appKey: String) {
    KakaoSdk.init(context, appKey)
}