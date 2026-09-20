package org.example.myapp.auth.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [PostEntity::class, FeedItemEntity::class],
    version = 1
)
@ConstructedBy(PostDatabaseConstructor::class)
abstract class PostDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PostDatabaseConstructor : RoomDatabaseConstructor<PostDatabase> {
    override fun initialize(): PostDatabase
}