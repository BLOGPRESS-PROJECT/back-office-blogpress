package com.kobe.blogpress_api.model.user

enum class RoleType(
    val level: Int,
    val department: String,
    val libelle: String
){
    // Niveau système (niveau 1 - le plus élevé)
    ADMIN_SYSTEM(1, "SYSTEME", "Administrateur Système"),

    // Niveau User authentifies (niveau 2)
    USER(2, "SIMPLE_USER", "Utilisateur simple sans contenu"),

    // Niveau visiteur (niveau 3)
    VISITOR(3, "VISITOR", "Visiteur de l'application");

    fun hasHigherOrEqualLevel(other: RoleType): Boolean {
        return this.level <= other.level
    }

    fun canManageRole(other: RoleType): Boolean {
        return this.level < other.level
    }
}

object RolePermissionConfig {
    private val rolePermissions = mapOf(
        RoleType.ADMIN_SYSTEM to setOf(
            Permission.GOD_MODE,
            Permission.CREATE_ACCOUNT,
            Permission.RESET_PASSWORD,
            Permission.READ_USER,
            Permission.DELETE_USER,
            Permission.DEACTIVATE_USER,
            Permission.REACTIVATE_USER,

        ),

        RoleType.USER to setOf(
            //user management account
            Permission.RESET_PASSWORD,
            //action sur le contenu
            Permission.CREATE,Permission.READ,Permission.UPDATE,Permission.DELETE,
            Permission.LIKE,Permission.FAVORIS,Permission.COMMENT,Permission.FORWARD,

            //users network action
            Permission.FOLLOW, Permission.VIEW_PROFILE_ACCOUNT, Permission.VIEW_ACCOUNT,
        ),

        RoleType.VISITOR to setOf(
            //create account
            Permission.CREATE_ACCOUNT,
            //lire le contenu public
            Permission.READ,
        )
    )

    fun getPermissions(roleType: RoleType): Set<Permission> {
        return rolePermissions[roleType] ?: emptySet()
    }

    fun hasPermission(roleType: RoleType, permission: Permission): Boolean {
        return getPermissions(roleType).contains(permission)
    }
}


enum class DeactivationReason(val description: String) {
    ADMIN_DECISION("Désactivé par un administrateur"),
    SECURITY_BREACH("Tentatives de connexion suspectes"),
    UNAUTHORIZED_ACCESS("Tentatives d'accès non autorisées"),
    POLICY_VIOLATION("Violation des politiques de sécurité"),
    SUSPICIOUS_ACTIVITY("Activité suspecte détectée"),
    MANUAL_REVIEW("Nécessite une révision manuelle"),
    SYSTEM_SECURITY("Mesure de sécurité automatique")
}