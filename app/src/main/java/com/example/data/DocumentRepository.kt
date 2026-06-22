package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao
) {
    val allActiveDocuments: Flow<List<DocumentEntity>> = documentDao.getAllActiveDocuments()
    val allCategories: Flow<List<CategoryEntity>> = documentDao.getAllCategories()
    val trashedDocuments: Flow<List<DocumentEntity>> = documentDao.getTrashedDocuments()

    fun getDocumentById(id: Long): Flow<DocumentEntity?> = documentDao.getDocumentById(id)

    fun getCategoriesByParentId(parentId: Long): Flow<List<CategoryEntity>> =
        documentDao.getCategoriesByParentId(parentId)

    suspend fun insertDocument(document: DocumentEntity): Long = withContext(Dispatchers.IO) {
        documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        // delete local file
        try {
            val file = File(document.localUri)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e("Repository", "Error deleting local file: ${e.message}")
        }
        documentDao.deleteDocument(document)
    }

    suspend fun deleteDocumentById(id: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentByIdSync(id)
        if (doc != null) {
            deleteDocument(doc)
        }
    }

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        documentDao.insertCategory(category)
    }

    suspend fun deleteCategoryById(id: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteCategoryById(id)
    }

    /**
     * Checks if a document with exact same file name and storage size exists.
     */
    suspend fun isDuplicate(fileName: String, fileSize: Long): Boolean = withContext(Dispatchers.IO) {
        val docs = documentDao.getAllActiveDocuments().first()
        docs.any { it.fileName.equals(fileName, ignoreCase = true) && it.fileSize == fileSize }
    }

    /**
     * Copies a file from external Uri (e.g. Photo Picker/Storage) to app's secure internal storage.
     */
    suspend fun copyFileToSecureInternal(uri: Uri, destFileName: String): String = withContext(Dispatchers.IO) {
        val secureFolder = File(context.filesDir, "secure_vault")
        if (!secureFolder.exists()) secureFolder.mkdirs()

        val destFile = File(secureFolder, "${System.currentTimeMillis()}_$destFileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    }

    /**
     * Writes direct string text to a secure internal file (for text document / scans)
     */
    suspend fun writeStringToSecureInternal(content: String, destFileName: String): String = withContext(Dispatchers.IO) {
        val secureFolder = File(context.filesDir, "secure_vault")
        if (!secureFolder.exists()) secureFolder.mkdirs()

        val destFile = File(secureFolder, "${System.currentTimeMillis()}_$destFileName")
        destFile.writeText(content)
        destFile.absolutePath
    }

    /**
     * EXPORT/DOWNLOAD saved offline file to public system Downloads directory.
     */
    suspend fun downloadDocumentToPublicDownloads(document: DocumentEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.localUri)
            if (!sourceFile.exists()) {
                // If local file somehow doesn't exist, reconstruct it from content
                val secureFolder = File(context.filesDir, "secure_vault")
                if (!secureFolder.exists()) secureFolder.mkdirs()
                sourceFile.writeText(document.ocrText.ifEmpty { "Arkiv Saved File content for: ${document.name}" })
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, document.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, document.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to insert MediaStore download entry."))

                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Result.success("Downloaded to public 'Downloads' folder successfully.")
            } else {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsFolder.exists()) downloadsFolder.mkdirs()

                var targetFile = File(downloadsFolder, document.fileName)
                if (targetFile.exists()) {
                    targetFile = File(downloadsFolder, "${System.currentTimeMillis()}_${document.fileName}")
                }

                sourceFile.copyTo(targetFile, overwrite = true)
                Result.success("Downloaded to 'Downloads/${targetFile.name}' successfully.")
            }
        } catch (e: Exception) {
            Log.e("Repository", "Failed export: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Empty trashed documents that are older than config autoEmptyDays days.
     */
    suspend fun performTrashMaintenance(autoEmptyDays: Int) = withContext(Dispatchers.IO) {
        val trashed = documentDao.getTrashedDocumentsSync()
        val limitTime = System.currentTimeMillis() - (autoEmptyDays.toLong() * 24 * 60 * 60 * 1000)
        for (doc in trashed) {
            val deletedTime = doc.trashDeletedTime ?: 0L
            if (deletedTime < limitTime) {
                deleteDocument(doc)
            }
        }
    }

    /**
     * Populate standard categories & preloaded vault metadata if database empty.
     */
    suspend fun populatePrepopulatedDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentCategories = documentDao.getAllCategories().first()
        if (currentCategories.isNotEmpty()) return@withContext // already populated

        // 1. Insert default standard categories
        val defaultCats = listOf(
            "Identity", "Education", "Finance", "Medical",
            "Insurance", "Vehicle", "Property", "Receipts",
            "Travel", "Miscellaneous"
        )
        val catIdMap = mutableMapOf<String, Long>()

        for (catName in defaultCats) {
            val id = documentDao.insertCategory(CategoryEntity(name = catName, isDefault = true))
            catIdMap[catName] = id
        }

        // 2. Insert nested structures for Education -> School, College -> Semester 1, 2, Certificates
        val eduId = catIdMap["Education"]
        if (eduId != null) {
            val schoolId = documentDao.insertCategory(CategoryEntity(name = "School", parentId = eduId))
            val collegeId = documentDao.insertCategory(CategoryEntity(name = "College", parentId = eduId))
            documentDao.insertCategory(CategoryEntity(name = "Semester 1", parentId = collegeId))
            documentDao.insertCategory(CategoryEntity(name = "Semester 2", parentId = collegeId))
            documentDao.insertCategory(CategoryEntity(name = "Certificates", parentId = collegeId))
        }

        // 3. Prepopulate default mock documents matching dashboard specs
        val preloadedDocs = listOf(
            DocumentEntity(
                name = "Aadhar Card",
                fileName = "aadhar_card.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: NATIONAL IDENTITY CARD\nISSUED BY: GOVERNMENT OF INDIA\nDOCUMENT ID: **** **** 8921\nHOLDER NAME: Alex Johnson\nDOB: 15/08/1995\nADDRESS: 142 Blue Sky Boulevard, New Delhi, India.",
                    "aadhar_card.txt"
                ),
                fileSize = 1200000, // 1.2 MB in bytes
                mimeType = "text/plain",
                category = "Identity",
                tags = "Important,Identity",
                ocrText = "DOCUMENT TYPE: NATIONAL IDENTITY CARD ISSUED BY: GOVERNMENT OF INDIA DOCUMENT ID: **** **** 8921 HOLDER NAME: Alex Johnson Address: 142 Blue Sky Boulevard",
                notes = "Scanned physical copy. Keep locked securely.",
                addedDate = System.currentTimeMillis() - 48 * 3600 * 1000 // 2 days ago
            ),
            DocumentEntity(
                name = "PAN Card",
                fileName = "pan_card.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: PERMANENT ACCOUNT NUMBER (PAN)\nISSUED BY: INCOME TAX DEPARTMENT, INDIA\nCARD NO: AFKJP8218D\nHOLDER NAME: Alex Johnson\nDATE OF ISSUE: 05/09/2018.",
                    "pan_card.txt"
                ),
                fileSize = 845000, // 845 KB
                mimeType = "text/plain",
                category = "Finance",
                tags = "Tax,Important",
                ocrText = "DOCUMENT TYPE: PERMANENT ACCOUNT NUMBER INCOME TAX DEPARTMENT INDIA CARD NO: AFKJP8218D ALEX JOHNSON 05/09/2018",
                notes = "For income tax filings and fast proof.",
                addedDate = System.currentTimeMillis() - 72 * 3600 * 1000 // 3 days ago
            ),
            DocumentEntity(
                name = "Driving License",
                fileName = "driving_license.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: DRIVING LICENSE\nLICENSE NO: DL-14202300589\nISSUED BY: TRANSPORT DEPARTMENT\nHOLDER NAME: Alex Johnson\nVALID TILL: 22/01/2039.",
                    "driving_license.txt"
                ),
                fileSize = 2100000, // 2.1 MB
                mimeType = "text/plain",
                category = "Vehicle",
                tags = "Important",
                ocrText = "DRIVING LICENSE NO: DL-14202300589 ISSUED BY: TRANSPORT DEPARTMENT ALEX JOHNSON VALID TILL 22/01/2039",
                notes = "Laminated card. Keep backup.",
                addedDate = System.currentTimeMillis() - 120 * 3600 * 1000 // 5 days ago
            ),
            DocumentEntity(
                name = "Registration Certificate",
                fileName = "rc_book.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: VEHICLE REGISTRATION CERTIFICATE\nREGISTRATION NO: DL-3C-AL-9912\nCHASSIS NO: MD364H8S62\nENGINE NO: EN826315\nHOLDER NAME: Alex Johnson.",
                    "rc_book.txt"
                ),
                fileSize = 450000, // 450 KB
                mimeType = "text/plain",
                category = "Vehicle",
                tags = "Vehicle",
                ocrText = "VEHICLE REGISTRATION CERTIFICATE REGISTRATION NO: DL-3C-AL-9912 CHASSIS NO: MD364H8S62 ENGINE NO: EN826315 ALEX JOHNSON",
                notes = "Primary sedan registration.",
                addedDate = System.currentTimeMillis() - 10 * 24 * 3600 * 1000 // 10 days ago
            ),
            DocumentEntity(
                name = "Passport",
                fileName = "passport.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: INTERNATIONAL PASSPORT\nPASSPORT NO: Z9182374\nISSUING COUNTRY: UNITED STATES\nHOLDER NAME: Alex Johnson\nVALID TILL: 15/08/2031.",
                    "passport.txt"
                ),
                fileSize = 5400000, // 5.4 MB
                mimeType = "text/plain",
                category = "Travel",
                tags = "Travel,Important",
                ocrText = "PASSPORT OFFICE USA PASSPORT NO: Z9182374 ALEX JOHNSON VALID TILL 15/08/2031 UNITED STATES OF AMERICA",
                notes = "10 years validity passport scan.",
                addedDate = System.currentTimeMillis() - 30 * 24 * 3600 * 1000 // 30 days ago
            ),
            DocumentEntity(
                name = "Health Insurance",
                fileName = "health_insurance.txt",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: MEDICAL INSURANCE POLICY SCHEDULE\nPOLICY NO: HI-9273615\nINSURER: BLUE SHIELD ACCORD\nHOLDER NAME: Alex Johnson\nPLAN TYPE: Individual Comprehensive\nCOVERAGE: 500,000 USD",
                    "health_insurance.txt"
                ),
                fileSize = 1800000, // 1.8 MB
                mimeType = "text/plain",
                category = "Insurance",
                tags = "Medical,Tax",
                ocrText = "MEDICAL INSURANCE POLICY SCHEDULE POLICY NO: HI-9273615 BLUE SHIELD ACCORD ALEX JOHNSON COVERAGE 500k USD",
                notes = "Direct cash payment hospital code is blue-shield-118.",
                addedDate = System.currentTimeMillis() - 15 * 24 * 3600 * 1000 // 15 days ago
            ),
            DocumentEntity(
                name = "Tax_Return_2023.pdf",
                fileName = "Tax_Return_2023.pdf",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: TAX FILING STATEMENTS (PDF)\nASSESSMENT YEAR: 2023-2024\nFILER NAME: Alex Johnson\nSTATUS: ACCEPTED BY REVENUE COMMISSIONERS\nTOTAL NET TAXABLE BASE: 84,000 USD\nSTAMP: INBOUND_TAX_OK_2023.",
                    "Tax_Return_2023.pdf"
                ),
                fileSize = 1500000, // 1.5 MB
                mimeType = "application/pdf",
                category = "Finance",
                tags = "Tax",
                ocrText = "TAX RETURNS PDF ASSESSMENT YEAR 2023 2024 FILER ALEX JOHNSON REVENUE OK STAMP INBOUND TAX OK 2023",
                notes = "Tax acknowledgments.",
                addedDate = System.currentTimeMillis() - 2 * 3600 * 1000 // 2 hours ago
            ),
            DocumentEntity(
                name = "Passport_Scan_Front.jpg",
                fileName = "Passport_Scan_Front.jpg",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: JPEG IMAGE SCANS - PASSPORT FRONT PAGE\nISSUED BY: DEPT OF STATE\nNUMBER: Z9182374\nNAME: Alex Johnson\nISSUED: 16/08/2021\nEXPIRES: 15/08/2031",
                    "Passport_Scan_Front.jpg"
                ),
                fileSize = 2300000, // 2.3 MB
                mimeType = "image/jpeg",
                category = "Identity",
                tags = "Travel",
                ocrText = "PASSPORT JPG SCANS DEPT OF STATE NUMBER Z9182374 ALEX JOHNSON 2021 2031",
                notes = "Primary picture scan for visual identity.",
                addedDate = System.currentTimeMillis() - 24 * 3600 * 1000 // Yesterday
            ),
            DocumentEntity(
                name = "Rental_Agreement.docx",
                fileName = "Rental_Agreement.docx",
                localUri = writeStringToSecureInternal(
                    "DOCUMENT TYPE: WORD FORMAT DOCX - APARTMENT RENTAL LEASE\nLOCATION: APARTMENT 4B, 82 WATERFRONT WAY, BOSTON, MA\nLANDLORD: Robert Smith LLC\nTENANT: Alex Johnson\nTERM: 01/01/2024 TO 31/12/2024\nRENT RATE: 2,400 USD PER MONTH\nSECURITY DEPOSIT: 4,800 USD",
                    "Rental_Agreement.docx"
                ),
                fileSize = 1100000, // 1.1 MB
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                category = "Property",
                tags = "Tax,Property",
                ocrText = "APARTMENT RENTAL LEASE WATERFRONT WAY BOSTON LANDLORD ROBERT SMITH TENANT ALEX JOHNSON RENT 2400 MONTH",
                notes = "Signed online lease docx.",
                addedDate = System.currentTimeMillis() - 72 * 3600 * 1000 // 3 days ago
            )
        )

        for (doc in preloadedDocs) {
            documentDao.insertDocument(doc)
        }
    }
}
