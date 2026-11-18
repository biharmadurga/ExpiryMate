package com.expirymate.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "expiry_items")
data class ExpiryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val expiryMillis: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ExpiryDao {
    @Query("SELECT * FROM expiry_items ORDER BY expiryMillis ASC")
    fun getAll(): Flow<List<ExpiryItem>>

    @Insert
    suspend fun insert(item: ExpiryItem)

    @Delete
    suspend fun delete(item: ExpiryItem)
}

@Database(entities = [ExpiryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expiryDao(): ExpiryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expirymate.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
