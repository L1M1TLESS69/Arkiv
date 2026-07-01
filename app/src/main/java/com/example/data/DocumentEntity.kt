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
    val isPinned: Boolean = false,
    val summary: String = "",
    val sortOrder: Int = 0
) : Serializable {
    // Custom getters mapping the entity fields to the requested data structure fields
    val title: String get() = name
    val content: String get() = notes.ifEmpty { ocrText }
    val tagsList: List<String> get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val timestamp: Long get() = addedDate
}

/**
 * Domain-level data structure representing a secured document with fields for
 * title, content, category, tags, and timestamp.
 */
data class Document(
    val id: Long,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String>,
    val timestamp: Long
)

/**
 * Extension mapper function to convert the database entity [DocumentEntity] to the domain [Document] data structure.
 */
fun DocumentEntity.toDomainDocument(): Document {
    return Document(
        id = id,
        title = title,
        content = content,
        category = category,
        tags = tagsList,
        timestamp = timestamp
    )
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null, // Parent category ID to support nested folders
    val isDefault: Boolean = false
) : Serializable
