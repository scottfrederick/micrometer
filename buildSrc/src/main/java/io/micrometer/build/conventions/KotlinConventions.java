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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

/**
 * Conventions that are applied in the presence of the Kotlin plugin to apply and
 * configure the {@code kotlinCompile} plugin.
 */
class KotlinConventions {

    private static final String API_AND_LANGUAGE_VERSION = "1.7";

    void apply(Project project) {
        project.getPlugins().withId("org.jetbrains.kotlin.jvm", (kotlinPlugin) -> project.getTasks()
                .withType(KotlinCompile.class, this::configureKotlinCompileConventions));
    }

    private void configureKotlinCompileConventions(KotlinCompile compile) {
        KotlinJvmOptions kotlinOptions = compile.getKotlinOptions();
        kotlinOptions.setApiVersion(API_AND_LANGUAGE_VERSION);
        kotlinOptions.setLanguageVersion(API_AND_LANGUAGE_VERSION);
        kotlinOptions.setJvmTarget(JavaConventions.TARGET_COMPATIBILITY.toString());
    }

}
