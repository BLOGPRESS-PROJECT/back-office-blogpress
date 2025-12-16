package com.kobe.blogpress_api.dto.user

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class BatchCreateUsersRequestDTO(
    @field:Min(1, message = "Count must be at least 1")
    @field:Max(100, message = "Count cannot exceed 100")
    val count: Int = 10,
    
    val passwordPrefix: String = "TestUser", // Le mot de passe sera "TestUser123!" + index
    val makeSomeGolden: Boolean = false, // Si true, certains utilisateurs seront Golden
    val goldenPercentage: Int = 10 // Pourcentage d'utilisateurs Golden (si makeSomeGolden = true)
)

