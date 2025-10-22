package com.kobe.blogpress_api.model.user

enum class Permission {
    //===========Full control permission================//
    GOD_MODE,

    //================USER MANAGEMENT==================//
    CREATE_ACCOUNT,
    RESET_PASSWORD,
    READ_USER,
    DELETE_USER,
    DEACTIVATE_USER,
    REACTIVATE_USER,

    //================CRUD sur le contenu==================//
    CREATE, UPDATE, DELETE, READ,

    //================Action sur les contenus==================//
    LIKE, FAVORIS, COMMENT, FORWARD,

    //================Action sur les user==================//
    FOLLOW, VIEW_PROFILE_ACCOUNT, VIEW_ACCOUNT,
}