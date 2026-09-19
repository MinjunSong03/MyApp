package org.example.myapp.auth.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("""
        SELECT p.* FROM posts p
        INNER JOIN feed_items f ON p.id = f.postId
        WHERE f.feedType = :feedType
        ORDER BY f.position ASC
    """)
    fun getFeed(feedType: String): Flow<List<Post>>

    @Upsert
    suspend fun upsertPosts(posts: List<Post>)

    @Upsert
    suspend fun upsertFeedItems(items: List<FeedItemEntity>)

    @Query("DELETE FROM feed_items WHERE feedType = :feedType")
    suspend fun clearFeed(feedType: String)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: Long)

    @Query("DELETE FROM feed_items WHERE feedType = :feedType AND postId = :postId")
    suspend fun removePostFromFeed(feedType: String, postId: Long)

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun deletePostsByUserId(userId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM feed_items WHERE feedType = :feedType")
    suspend fun getNextPosition(feedType: String): Int

    @Transaction
    suspend fun saveFeedPage(
        feedType: String,
        posts: List<Post>,
        isRefresh: Boolean
    ) {
        if (isRefresh) {
            clearFeed(feedType)
        }
        upsertPosts(posts)

        val startPos = if (isRefresh) 0 else getNextPosition(feedType)
        val feedItems = posts.mapIndexed { index, post ->
            FeedItemEntity(
                feedType = feedType,
                postId = post.id,
                position = startPos + index
            )
        }
        upsertFeedItems(feedItems)
    }
}