package com.kobe.blogpress_api.domain.interaction

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "favorites")
@CompoundIndexes(
    CompoundIndex(name = "content_user_idx", def = "{'contentId': 1, 'userId': 1}", unique = true)
)
data class Favorite(
    @Id
    val id: ObjectId = ObjectId(),

    val contentId: ObjectId,

    val contentType: ContentType,

    val userId: ObjectId,

    val createdAt: Instant = Instant.now()
)