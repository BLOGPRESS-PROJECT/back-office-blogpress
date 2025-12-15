package com.kobe.blogpress_api.domain.model.storage

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Modèle pour tracker le quota de stockage par utilisateur.
 * Les Golden Users ont un quota illimité (ou très élevé).
 */
@Document(collection = "storage_quotas")
data class StorageQuota(
    @Id
    val id: ObjectId = ObjectId(),

    @Indexed(unique = true)
    val userId: ObjectId,

    val usedStorageBytes: Long = 0, // Stockage utilisé en bytes
    val maxStorageBytes: Long = 524288000, // 500 MB par défaut (standard users)
    val isUnlimited: Boolean = false, // true pour Golden Users

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Vérifie si l'utilisateur peut encore stocker des fichiers.
     */
    fun canStore(bytes: Long): Boolean {
        return isUnlimited || (usedStorageBytes + bytes <= maxStorageBytes)
    }

    /**
     * Retourne le pourcentage d'utilisation du quota.
     */
    fun getUsagePercentage(): Double {
        if (isUnlimited) return 0.0
        if (maxStorageBytes == 0L) return 0.0
        return (usedStorageBytes.toDouble() / maxStorageBytes.toDouble()) * 100.0
    }

    /**
     * Retourne l'espace restant disponible en bytes.
     */
    fun getRemainingBytes(): Long {
        return if (isUnlimited) Long.MAX_VALUE else maxOf(0, maxStorageBytes - usedStorageBytes)
    }
}

