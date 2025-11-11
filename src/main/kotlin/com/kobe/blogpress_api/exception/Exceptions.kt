package com.kobe.blogpress_api.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
class ResourceAlreadyExistsException(message: String) : RuntimeException(message)
class AuthenticationException(message: String) : RuntimeException(message)
class AuthorizationException(message: String) : RuntimeException(message)
class FileStorageException(message: String) : RuntimeException(message)

class ValidationException(message: String, val errors: Map<String, String> = emptyMap()) : RuntimeException(message)