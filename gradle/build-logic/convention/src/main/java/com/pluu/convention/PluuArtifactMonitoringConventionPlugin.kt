package com.pluu.convention

import com.pluu.convention.buildService.PluuReporterService
import com.pluu.convention.buildService.PluuTaskCheckService
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluuArtifactMonitoringConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureTestTasks()
        }
    }

    private fun Project.configureTestTasks() {
        val buildTaskService = PluuTaskCheckService.registerService(gradle)
        PluuReporterService.registerService(gradle, buildTaskService)
    }
}
