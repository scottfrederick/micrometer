/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task to extract resources from the classpath and write them to disk.
 */
public class ExtractResources extends DefaultTask {

    private final DirectoryProperty destinationDirectory;

    private List<String> resourceNames = new ArrayList<>();

    public ExtractResources() {
        this.destinationDirectory = getProject().getObjects().directoryProperty();
    }

    @Input
    public List<String> getResourceNames() {
        return this.resourceNames;
    }

    public void setResourcesNames(List<String> resourceNames) {
        this.resourceNames = resourceNames;
    }

    @OutputDirectory
    public DirectoryProperty getDestinationDirectory() {
        return this.destinationDirectory;
    }

    @TaskAction
    void extractResources() throws IOException {
        for (String resourceName : this.resourceNames) {
            File resourceFile = this.getProject().getRootProject().file(resourceName);
            if (!resourceFile.exists()) {
                throw new GradleException("Resource '" + resourceFile.getAbsolutePath() + "' does not exist");
            }
            Path destinationPath = this.destinationDirectory.file(resourceName).get().getAsFile().toPath();
            Files.copy(resourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
