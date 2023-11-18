package com.pluu.convention.buildService

import com.pluu.convention.utils.serviceRegistry
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent

abstract class PluuTaskCheckService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener {

    private val findArtifactRegex = "(?<=Could not find ).+(?=\\.)".toRegex()

    private var _failureArtifacts: MutableSet<String> = mutableSetOf()
    val failureArtifacts: Set<String> = _failureArtifacts

    val buildPhaseFailed: Boolean
        get() = failureArtifacts.isNotEmpty()

    override fun onFinish(event: FinishEvent?) {
        if (event == null || event !is TaskFinishEvent)
            return
        val result = event.result
        if (result is TaskFailureResult) {
            if (result.failures.isNullOrEmpty()) return
            _failureArtifacts.addAll(
                result.failures.flatMap { failure ->
                    failure.description?.let { desc ->
                        findArtifactRegex.findAll(desc).flatMap { it.groupValues }
                    }.orEmpty()
                }
            )
        }
    }

    companion object {
        fun registerService(gradle: Gradle): Provider<PluuTaskCheckService> {
            val registry = gradle.serviceRegistry()[BuildEventListenerRegistryInternal::class.java]
            val buildDurationService = gradle.sharedServices.registerIfAbsent(
                "build-task-service",
                PluuTaskCheckService::class.java
            ) { }
            registry.onTaskCompletion(buildDurationService)
            return buildDurationService
        }
    }
}
