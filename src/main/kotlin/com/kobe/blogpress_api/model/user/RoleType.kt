package com.kobe.blogpress_api.model.user

enum class RoleType(
    val level: Int,
    val department: String,
    val libelle: String
){
    // Niveau système (niveau 1 - le plus élevé)
    ADMIN_SYSTEM(1, "SYSTEME", "Administrateur Système"),

    // Niveau User authentifies (niveau 2)
    AUTHOR_USER(2, "AUTHOR_USER", "Utilisateur createur de contenu"),
    SIMPLE_USER(2, "SIMPLE_USER", "Utilisateur simple sans contenu"),

    // Niveau visiteur (niveau 3)
    VISITOR(3, "RH", "Directeur des Ressources Humaines");

    fun hasHigherOrEqualLevel(other: RoleType): Boolean {
        return this.level <= other.level
    }

    fun canManageRole(other: RoleType): Boolean {
        return this.level < other.level
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