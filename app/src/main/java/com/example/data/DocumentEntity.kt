package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fileName: String,
    val localUri: String, // Path to internal file
    val fileSize: Long,
    val mimeType: String,
    val category: String, // e.g. "Identity", "Education"
    val parentFolderId: Long? = null, // for nested subfolders inside categories
    val addedDate: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val trashDeletedTime: Long? = null,
    val ocrText: String = "",
    val tags: String = "", // Comma-separated tags
    val isVerified: Boolean = true,
    val notes: String = "",
    val isPinned: Boolean = false
) : Serializable

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null, // Parent category ID to support nested folders
    val isDefault: Boolean = false
) : Serializable
