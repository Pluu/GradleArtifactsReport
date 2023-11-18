package com.pluu.convention.utils

import org.gradle.api.invocation.Gradle
import org.gradle.internal.service.ServiceRegistry
import org.gradle.invocation.DefaultGradle

fun Gradle.serviceRegistry(): ServiceRegistry = (this as DefaultGradle).services