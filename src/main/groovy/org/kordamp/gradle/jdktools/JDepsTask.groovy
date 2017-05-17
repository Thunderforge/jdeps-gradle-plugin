/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle.jdktools

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException
import org.zeroturnaround.exec.ProcessExecutor

/**
 * @author Andres Almiray
 */
class JDepsTask extends DefaultTask {
    @Input boolean failOnError = true
    @Input boolean verbose = false
    @Input boolean summary = false
    @Input boolean profile = false
    @Input boolean recursive = false
    @Input boolean jdkinternals = true

    List<String> failures = []

    @TaskAction
    void evaluate() {
        final List<String> baseCmd = ['jdeps']
        if (summary) baseCmd << '-s'
        if (verbose) baseCmd << '-v'
        if (profile) baseCmd << '-profile'
        if (recursive) baseCmd << '-recursive'
        if (jdkinternals) baseCmd << '-jdkinternals'

        project.sourceSets.main.output.files.each { File file ->
            if (!file.exists()) {
                return // skip
            }

            String output = JDepsTask.runJDepsOn(baseCmd, file.absolutePath)

            if (output) {
                failures << "${project.name}\n$output"
            }
        }

        project.configurations.runtime.resolve().each { File file ->
            if (!file.exists()) {
                return // skip
            }

            String output = JDepsTask.runJDepsOn(baseCmd, file.absolutePath)

            if (output) {
                failures << "$output"
            }
        }

        if (failures) println failures.join('\n')
        if (failOnError && failures) throw new BuildException("jdeps reported errors in ${project.name}".toString(), null)
    }

    private static String runJDepsOn(List<String> baseCmd, String path) {
        List<String> cmd = []
        cmd.addAll(baseCmd)
        cmd.add(path)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        new ProcessExecutor(cmd).redirectOutput(out).execute().getExitValue()
        return out.toString().trim()
    }
}
