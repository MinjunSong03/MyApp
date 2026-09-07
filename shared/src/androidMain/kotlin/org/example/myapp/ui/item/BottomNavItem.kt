package org.example.myapp.ui.item

import androidx.annotation.DrawableRes
import org.example.myapp.shared.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val icon: Int
) {
    object Home : BottomNavItem("home", "홈", R.drawable.ic_home)
    object CreatePost: BottomNavItem("create_post", "", R.drawable.ic_add)
    object MyInfo : BottomNavItem("my_info", "내 정보", R.drawable.ic_my_info)
}