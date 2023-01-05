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

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import io.spring.javaformat.gradle.tasks.Format;
import io.spring.nohttp.gradle.NoHttpPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

import java.util.Collections;

/**
 * Conventions that are applied in the presence of a {@link JavaBasePlugin} plugin
 * ({@code java} or {@code java-library}) to apply and configure the {@code checkstyle},
 * {@code io.spring.javaformat}, {@code io.spring.nohttp}, and
 * {@code com.diffplug.spotless} plugins.
 */
class StyleConventions {

    void apply(Project project) {
        project.getPlugins().withType(JavaBasePlugin.class, (javaPlugin) -> {
            configureSpringJavaFormat(project);
            configureCheckstyle(project);
            configureSpotless(project);
            configureNoHttp(project);
        });
    }

    private void configureSpringJavaFormat(Project project) {
        project.getPlugins().apply(SpringJavaFormatPlugin.class);
        project.getTasks().withType(Format.class, (formatTask) -> formatTask.setEncoding("UTF-8"));
    }

    private void configureCheckstyle(Project project) {
        project.getPlugins().apply(CheckstylePlugin.class);
        CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
        checkstyle.setToolVersion("8.45.1");
        checkstyle.getConfigDirectory().set(project.getRootProject().file("config/checkstyle"));
        DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
        checkstyleDependencies.add(
                project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:latest.release"));
    }

    private void configureSpotless(Project project) {
        project.getPlugins().apply(SpotlessPlugin.class);
        SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);
        spotless.kotlin((kotlin) -> kotlin.ktlint()
                .editorConfigOverride(Collections.singletonMap("ktlint_disabled_rules", "no-wildcard-imports")));
    }

    private void configureNoHttp(Project project) {
        project.getPlugins().apply(NoHttpPlugin.class);
    }

}
