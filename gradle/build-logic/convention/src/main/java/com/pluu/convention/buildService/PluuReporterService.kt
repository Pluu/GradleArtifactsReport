package com.pluu.convention.buildService

import com.pluu.convention.utils.serviceRegistry
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
        val buildReport = getBuildReport(buildTaskService) ?: return
        val logger = Logging.getLogger("console-logger")
        logger.lifecycle(buildReport)
    }

    private fun getBuildReport(
        buildTaskService: PluuTaskCheckService,
    ): String? {
        val buildFailureMessage = if (buildTaskService.buildPhaseFailed) {
            buildString {
                appendLine("▼▼▼▼ Not found Artifacts ▼▼▼▼")
                buildTaskService.failureArtifacts.forEach {
                    appendLine(it)
                }
                appendLine("▲▲▲▲ Not found Artifacts ▲▲▲▲▲")
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