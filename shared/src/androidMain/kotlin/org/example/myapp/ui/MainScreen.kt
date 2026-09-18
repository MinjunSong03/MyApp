package org.example.myapp.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.example.myapp.ui.item.BottomNavItem

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    var backPressedTime by rememberSaveable { mutableStateOf(0L) }

    val homeListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.CreatePost,
        BottomNavItem.MyInfo
    )

    val isTopLevelTab = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (isTopLevelTab) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        ),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (item.route == "home") {
                                        coroutineScope.launch {
                                            homeListState.animateScrollToItem(0)
                                        }
                                    }
                                } else {
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
                                    contentDescription = item.title,
                                )
                            },
                            label = {
                                Text(text = item.title)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { fadeIn(animationSpec = tween(100)) },
                exitTransition = { fadeOut(animationSpec = tween(100)) },
                popEnterTransition = { fadeIn(animationSpec = tween(100)) },
                popExitTransition = { fadeOut(animationSpec = tween(100)) }
            ) {
                composable("blocked_user") {
                    BlockedUserScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("create_post") {
                    CreatePostScreen(
                        onCreatePostClick = {
                            navController.popBackStack()
                        }
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
                composable("edit_profile") {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("home") {
                    HomeScreen(
                        listState = homeListState,
                        onNavigateToPostDetail = { postId ->
                            navController.navigate("post_detail/$postId")
                        },
                        onNavigateToEditPost = { postId ->
                            navController.navigate("edit_post/$postId")
                        },
                        onNavigateToProfileClick = { userId ->
                            navController.navigate("profile_click/$userId")
                        }
                    )
                }
                composable(
                    route = "post_detail/{postId}",
                    arguments = listOf(navArgument("postId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getLong("postId") ?: return@composable
                    PostDetailScreen(
                        postId = postId,
                        onBack = { navController.popBackStack() },
                        onNavigateToEditPost = { id ->
                            navController.navigate("edit_post/$id")
                        },
                        onNavigateToProfileClick = {userId ->
                            navController.navigate("profile_click/$userId")}
                    )
                }
                composable("my_info") {
                    MyInfoScreen(
                        onUpdateNicknameClick = { navController.navigate("edit_profile") },
                        onMyPostClick = { navController.navigate("post_my") },
                        onManageMyClick = { navController.navigate("blocked_user")},
                        onLicenseClick = { navController.navigate("license")}
                    )
                }
                composable("post_my") {
                    MyPostScreen(
                        onNavigateToEditPost = { postId ->
                            navController.navigate("edit_post/$postId") },
                        onNavigateToPostDetail = { postId ->
                            navController.navigate("post_detail/$postId")
                        },
                        onNavigateToProfileClick = { userId ->
                            navController.navigate("profile_click/$userId")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "profile_click/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getLong("userId") ?: return@composable
                    ProfileClickScreen(
                        userId = userId,
                        onBack = { navController.popBackStack() },
                        onNavigateToPostDetail = { postId ->
                            navController.navigate("post_detail/$postId")
                        },
                        onNavigateToEditPost = { postId ->
                            navController.navigate("edit_post/$postId")
                        }
                    )
                }
                composable("license") {
                    LicenseScreen(
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