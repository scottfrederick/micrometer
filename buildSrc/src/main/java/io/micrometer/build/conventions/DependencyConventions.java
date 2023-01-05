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

package io.micrometer.build.conventions;

import io.micrometer.build.conventions.tasks.DownloadDependencies;
import io.micrometer.build.conventions.tasks.ResolveAndLockDependencies;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileTree;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Delete;

/**
 * Conventions that configure dependency management.
 */
class DependencyConventions {

    private static final Spec<Task> NEVER = (task) -> false;

    void apply(Project project) {
        configureDependencyLocking(project);
        registerResolveAndLockAllTask(project);
        registerDownloadDependenciesTask(project);
        createDeleteLockFilesTask(project);
    }

    private void configureDependencyLocking(Project project) {
        project.getDependencyLocking().lockAllConfigurations();
    }

    private void registerResolveAndLockAllTask(Project project) {
        project.getTasks().register("resolveAndLockAll", ResolveAndLockDependencies.class, (task) -> {
            task.setDescription("Resolves dependencies of all configurations and writes them into the lock file.");
            task.getOutputs().upToDateWhen(NEVER);
        });
    }

    private void registerDownloadDependenciesTask(Project project) {
        project.getTasks().register("downloadDependencies", DownloadDependencies.class, (task) -> {
            task.setDescription("Download all dependencies");
            task.getOutputs().upToDateWhen(NEVER);
        });
    }

    private void createDeleteLockFilesTask(Project project) {
        project.getTasks().create("deleteLockFiles", Delete.class, (deleteTask) -> {
            FileTree lockFiles = project.fileTree(project.getRootDir(), (files) -> files.include("**/gradle.lockfile"));
            deleteTask.delete(lockFiles);
        });
    }

}
