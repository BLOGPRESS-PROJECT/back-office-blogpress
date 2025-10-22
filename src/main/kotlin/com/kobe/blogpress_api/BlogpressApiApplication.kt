package com.kobe.blogpress_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableReactiveMongoRepositories
@ConfigurationPropertiesScan
@SpringBootApplication
class BlogpressApiApplication

fun main(args: Array<String>) {
	runApplication<BlogpressApiApplication>(*args)
}