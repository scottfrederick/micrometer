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

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Conventions that are applied in the presence of a {@link JavaPlugin} plugin to apply
 * and configure project input normalization.
 */
class NormalizationConventions {

    private static final List<String> KEYS_TO_IGNORE = Arrays.asList("Build-Date", "Build-Date-UTC", "Built-By",
            "Built-OS", "Build-Host", "Build-Job", "Build-Number", "Build-Id", "Change", "Full-Change", "Branch",
            "Module-Origin", "Created-By", "Build-Java-Version");

    void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> project.getNormalization().runtimeClasspath(
                (runtimeClasspath) -> runtimeClasspath.metaInf((metaInf) -> KEYS_TO_IGNORE.forEach((key) -> {
                    metaInf.ignoreAttribute(key);
                    metaInf.ignoreProperty(key);

                }))));
    }

}
