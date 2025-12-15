package com.kobe.blogpress_api.services.storage

import com.kobe.blogpress_api.domain.model.storage.StorageQuota
import com.kobe.blogpress_api.repository.storage.StorageQuotaRepository
import com.kobe.blogpress_api.services.user.UserService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service pour gérer les quotas de stockage des utilisateurs.
 * Les Golden Users ont un quota illimité.
 */
@Service
class StorageQuotaService(
    private val storageQuotaRepository: StorageQuotaRepository,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(StorageQuotaService::class.java)

    companion object {
        // Quotas par défaut
        const val DEFAULT_STORAGE_LIMIT_BYTES = 524288000L // 500 MB
        const val GOLDEN_STORAGE_LIMIT_BYTES = Long.MAX_VALUE // Illimité (ou très élevé)
    }

    /**
     * Récupère ou crée le quota pour un utilisateur.
     */
    suspend fun getOrCreateQuota(userId: ObjectId): StorageQuota {
        val existing = storageQuotaRepository.findByUserId(userId).awaitSingleOrNull()
        if (existing != null) {
            // Mettre à jour si l'utilisateur est devenu Golden entre-temps
            val user = userService.findById(userId)
            if (user.isGoldenUser && !existing.isUnlimited) {
                return updateQuotaToUnlimited(userId)
            }
            return existing
        }

        // Créer un nouveau quota
        val user = userService.findById(userId)
        val quota = StorageQuota(
            userId = userId,
            maxStorageBytes = if (user.isGoldenUser) GOLDEN_STORAGE_LIMIT_BYTES else DEFAULT_STORAGE_LIMIT_BYTES,
            isUnlimited = user.isGoldenUser
        )

        return storageQuotaRepository.save(quota).awaitSingle()
    }

    /**
     * Met à jour le quota pour qu'il soit illimité (quand un user devient Golden).
     */
    suspend fun updateQuotaToUnlimited(userId: ObjectId): StorageQuota {
        val quota = getOrCreateQuota(userId)
        val updated = quota.copy(
            isUnlimited = true,
            maxStorageBytes = GOLDEN_STORAGE_LIMIT_BYTES,
            updatedAt = Instant.now()
        )
        return storageQuotaRepository.save(updated).awaitSingle()
    }

    /**
     * Vérifie si l'utilisateur peut stocker un fichier de taille donnée.
     */
    suspend fun canStoreFile(userId: ObjectId, fileSizeBytes: Long): Boolean {
        val quota = getOrCreateQuota(userId)
        return quota.canStore(fileSizeBytes)
    }

    /**
     * Ajoute de l'espace utilisé au quota.
     */
    suspend fun addStorageUsage(userId: ObjectId, bytes: Long): StorageQuota {
        val quota = getOrCreateQuota(userId)
        val updated = quota.copy(
            usedStorageBytes = quota.usedStorageBytes + bytes,
            updatedAt = Instant.now()
        )
        return storageQuotaRepository.save(updated).awaitSingle()
    }

    /**
     * Retire de l'espace utilisé du quota (quand un fichier est supprimé).
     */
    suspend fun removeStorageUsage(userId: ObjectId, bytes: Long): StorageQuota {
        val quota = getOrCreateQuota(userId)
        val updated = quota.copy(
            usedStorageBytes = maxOf(0, quota.usedStorageBytes - bytes),
            updatedAt = Instant.now()
        )
        return storageQuotaRepository.save(updated).awaitSingle()
    }

    /**
     * Récupère le quota d'un utilisateur.
     */
    suspend fun getQuota(userId: ObjectId): StorageQuota {
        return getOrCreateQuota(userId)
    }

    /**
     * Calcule la taille totale des fichiers d'un utilisateur.
     * Utile pour recalculer le quota si nécessaire.
     */
    suspend fun calculateUsedStorage(userId: ObjectId): Long {
        // TODO: Implémenter le calcul réel en parcourant tous les fichiers de l'utilisateur
        // Pour l'instant, on se base sur le quota stocké
        val quota = getOrCreateQuota(userId)
        return quota.usedStorageBytes
    }
}

