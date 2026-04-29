package com.example.recipebookappandorid.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recipebookappandorid.data.local.entity.SharedBookInviteEntity

@Dao
interface SharedBookInviteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvites(invites: List<SharedBookInviteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: SharedBookInviteEntity)

    @Query("SELECT * FROM shared_book_invites ORDER BY createdAt DESC")
    fun getAllInvites(): LiveData<List<SharedBookInviteEntity>>

    @Query("DELETE FROM shared_book_invites")
    suspend fun clearInvites()
}
