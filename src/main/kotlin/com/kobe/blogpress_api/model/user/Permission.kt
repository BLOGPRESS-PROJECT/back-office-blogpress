package com.kobe.blogpress_api.model.user

enum class Permission {
    //Full control permission
    GOD_MODE,

    // Gestion des utilisateurs
    CREATE_USER,
    READ_USER,
    GET_ALL_USERS,
    FULL_USER_CONTROL,
    UPDATE_USER,
    DELETE_USER,
    RESET_PASSWORD,
    ACTIVATE_USER,
    DEACTIVATE_USER,

    // Requetes utilisateur
    CREATE_REQUEST,
    READ_ALL_REQUEST,
    READ_OWN_REQUESTS,
    UPDATE_REQUEST,
    DELETE_REQUEST,
    REVIEW_REQUEST_APPROVE_REQUEST,
    CANCEL_REQUEST,


    // Gestion des rôles
    ASSIGN_ROLE,
    MODIFY_ROLE,
}