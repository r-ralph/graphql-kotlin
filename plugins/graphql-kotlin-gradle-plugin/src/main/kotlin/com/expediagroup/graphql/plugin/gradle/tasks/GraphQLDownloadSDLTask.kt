/*
 * Copyright 2020 Expedia, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expediagroup.graphql.plugin.gradle.tasks

import com.expediagroup.graphql.plugin.config.TimeoutConfig
import com.expediagroup.graphql.plugin.downloadSchema
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

internal const val DOWNLOAD_SDL_TASK_NAME: String = "graphqlDownloadSDL"

/**
 * Task that attempts to download GraphQL schema in SDL format from the specified endpoint and save it locally.
 */
@Suppress("UnstableApiUsage")
open class GraphQLDownloadSDLTask : DefaultTask() {

    /**
     * Target GraphQL server SDL endpoint that will be used to download schema.
     */
    @Input
    @Option(option = "endpoint", description = "target SDL endpoint")
    val endpoint: Property<String> = project.objects.property(String::class.java)

    /**
     * Optional HTTP headers to be specified on a SDL request.
     */
    @Input
    val headers: MapProperty<String, Any> = project.objects.mapProperty(String::class.java, Any::class.java)

    /**
     * Timeout configuration that specifies maximum amount of time (in milliseconds) to connect and download schema from SDL endpoint before we cancel the request.
     * Defaults to Ktor CIO engine defaults (5 seconds for connect timeout and 15 seconds for read timeout).
     */
    @Input
    val timeoutConfig: Property<TimeoutConfig> = project.objects.property(TimeoutConfig::class.java)

    @OutputFile
    val outputFile: Provider<RegularFile> = project.layout.buildDirectory.file("schema.graphql")

    init {
        group = "GraphQL"
        description = "Download schema in SDL format from target endpoint."

        headers.convention(emptyMap())
        timeoutConfig.convention(TimeoutConfig())
    }

    /**
     * Download schema in SDL format from the specified endpoint and sve it locally in the target output file.
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
    @TaskAction
    fun downloadSDLAction() {
        logger.debug("starting download SDL task against ${endpoint.get()}")
        runBlocking {
            val schema = downloadSchema(endpoint = endpoint.get(), httpHeaders = headers.get(), timeoutConfig = timeoutConfig.get())
            val outputFile = outputFile.get().asFile
            outputFile.writeText(schema, Charsets.UTF_8)
        }
        logger.debug("successfully downloaded SDL")
    }
}
