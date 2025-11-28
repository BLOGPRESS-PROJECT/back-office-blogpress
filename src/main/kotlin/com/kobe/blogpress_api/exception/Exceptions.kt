package com.kobe.blogpress_api.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class ResourceAlreadyExistsException(message: String) : RuntimeException(message)
class AuthenticationException(message: String) : RuntimeException(message)
class AuthorizationException(message: String) : RuntimeException(message)
class FileStorageException(message: String) : RuntimeException(message)

class ValidationException(message: String, val errors: Map<String, String> = emptyMap()) : RuntimeException(message)

// Exceptions pour les blogs
class BlogNotFoundException(message: String = "Le blog recherché n'est pas disponible") : RuntimeException(message)
class BlogNotPublishedException(message: String = "Le blog recherché n'est pas encore disponible") : RuntimeException(message)
class BlogPrivateException(message: String = "Ce blog est privé") : RuntimeException(message)