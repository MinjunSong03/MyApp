package org.example.myapp.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    var backPressedTime by rememberSaveable { mutableStateOf(0L) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Menu,
        BottomNavItem.Home,
        BottomNavItem.MyInfo
    )

    val isTopLevelTab = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(text = item.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color.Black,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("menu") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "메뉴 화면", fontSize = 24.sp)
                    }
                }
                composable("home") {
                    HomeScreen(
                        onNavigateToCreatePost = {
                            navController.navigate("create_post")
                        },
                        onNavigateToEditPost = { postId ->
                            navController.navigate("edit_post/$postId")
                        }
                    )
                }
                composable("create_post") {
                    CreatePostScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "edit_post/{postId}",
                    arguments = listOf(navArgument("postId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getLong("postId") ?: return@composable
                    EditPostScreen(
                        postId = postId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("my_info") {
                    MyInfoScreen(
                        onUpdateNicknameClick = { navController.navigate("detail") },
                        onMyPostClick = { navController.navigate("post_my") },
                        onManageMyClick = { navController.navigate("manage_my")}
                    )
                }
                composable("detail") {
                    DetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("post_my") {
                    MyPostScreen(
                        onNavigateToEditPost = { postId ->
                            navController.navigate("edit_post/$postId") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("manage_my") {
                    ManageMyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
        BackHandler(enabled = isTopLevelTab) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000L) {
                activity?.moveTaskToBack(true)
            } else {
                backPressedTime = currentTime
            }
        }
    }
}