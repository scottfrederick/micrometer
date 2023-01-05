/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micrometer.build.conventions.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle task that resolves all project dependencies.
 */
public class ResolveAndLockDependencies extends DefaultTask {

    @TaskAction
    void resolve() {
        Project project = getProject();
        if (!project.getGradle().getStartParameter().isWriteDependencyLocks()
                && project.getGradle().getStartParameter().getLockedDependenciesToUpdate().isEmpty()) {
            throw new GradleException("Use --write-locks or --update-locks <dependencies> to lock dependencies");
        }
        project.getConfigurations().stream().filter(Configuration::isCanBeResolved).forEach(Configuration::resolve);
    }

}
