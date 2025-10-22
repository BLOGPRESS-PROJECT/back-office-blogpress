plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.kobe"
version = "0.0.1-SNAPSHOT"
description = "Blogpress Backend Services"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// Configuration pour exclure les dépendances conflictuelles
configurations.all {
	exclude(group = "org.slf4j", module = "slf4j-simple")
	exclude(group = "commons-logging", module = "commons-logging")
	exclude(group = "org.slf4j", module = "slf4j-log4j12")
}


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// AspectJ pour les annotations @Retryable et AOP
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.aspectj:aspectjweaver")

	// ========== CACHE REDIS (NOUVEAU - ESSENTIEL) ==========
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

	// Test dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.slf4j", module = "slf4j-simple")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()

	// ========== OPTIMISATION DES TESTS (NOUVEAU) ==========
	jvmArgs("-Xmx2g", "-XX:+UseG1GC")
	systemProperty("spring.profiles.active", "test")

	// Désactiver les tests qui utilisent les services externes en CI
	if (System.getenv("CI") == "true") {
		systemProperty("spring.test.context.cache.maxSize", "1")
	}
}