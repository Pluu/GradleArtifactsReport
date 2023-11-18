package com.pluu.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationListener
import org.gradle.internal.operations.OperationFinishEvent
import org.gradle.internal.operations.OperationIdentifier
import org.gradle.internal.operations.OperationProgressEvent
import org.gradle.internal.operations.OperationStartEvent
import org.gradle.internal.service.ServiceRegistry
import org.gradle.invocation.DefaultGradle
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureTestTasks()
        }
    }

    fun Project.configureTestTasks() {
        val buildTaskService = PluuTaskCheckService.registerService(gradle)
        PluuReporterService.registerService(gradle, buildTaskService)
    }
}

abstract class PluuTaskCheckService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener {

    private val findArtifactRegex = "(?<=Could not find ).+(?=\\.)".toRegex()

    var buildPhaseFailureMessage: List<String>? = null
    val buildPhaseFailed: Boolean
        get() = buildPhaseFailureMessage != null

    override fun onFinish(event: FinishEvent?) {
        if (event == null || event !is TaskFinishEvent)
            return
        val result = event.result
        if (result is TaskFailureResult) {
            if (result.failures.isNullOrEmpty()) return
            buildPhaseFailureMessage = result.failures.flatMap { failure ->
                failure.description?.let { desc ->
                    findArtifactRegex.findAll(desc).flatMap { it.groupValues }
                }.orEmpty()
            }
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

abstract class PluuReporterService : BuildService<PluuReporterService.Params>,
    BuildOperationListener, AutoCloseable {

    interface Params : BuildServiceParameters {
        fun getBuildTaskServiceProvider(): Property<Provider<PluuTaskCheckService>>
    }

    override fun started(p0: BuildOperationDescriptor, p1: OperationStartEvent) {}

    override fun progress(p0: OperationIdentifier, p1: OperationProgressEvent) {}

    override fun finished(
        buildOperationDescriptor: BuildOperationDescriptor,
        operationFinishEvent: OperationFinishEvent
    ) {
    }

    override fun close() {
        logBuildStats()
    }

    private fun logBuildStats() {
        val buildTaskService = parameters.getBuildTaskServiceProvider().get().get()
        val buildReport = getBuildReport(buildTaskService)
        val logger = Logging.getLogger("console-logger")
        logger.lifecycle(buildReport.toString())
    }

    private fun getBuildReport(
        buildTaskService: PluuTaskCheckService,
    ): String? {
        val buildFailureMessage = if (buildTaskService.buildPhaseFailed) {
            buildString {
                appendLine("▼▼▼▼ 해당 라이브러리를 다시 찾아보시오 ▼▼▼▼")
                buildTaskService.buildPhaseFailureMessage?.forEach {
                    appendLine(it)
                }
                appendLine("▲▲▲▲ 해당 라이브러리를 다시 찾아보시오 ▲▲▲▲▲")
            }
        } else {
            null
        }
        return buildFailureMessage
    }

    companion object {
        fun registerService(
            gradle: Gradle,
            buildTaskService: Provider<PluuTaskCheckService>
        ): Provider<PluuReporterService> {
            val registry = gradle.serviceRegistry()[BuildEventListenerRegistryInternal::class.java]
            val buildReporterService = gradle.sharedServices.registerIfAbsent(
                "build-reporter-service",
                PluuReporterService::class.java
            ) {
                parameters.getBuildTaskServiceProvider().set(buildTaskService)
            }
            registry.onOperationCompletion(buildReporterService)
            return buildReporterService
        }
    }
}

fun Gradle.serviceRegistry(): ServiceRegistry = (this as DefaultGradle).services