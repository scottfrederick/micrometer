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

import com.gradle.enterprise.gradleplugin.testretry.TestRetryExtension;
import io.micrometer.build.conventions.tasks.ExtractResources;
import io.spring.javaformat.gradle.tasks.CheckFormat;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Conventions that are applied in the presence of a {@link JavaPlugin} plugin
 * ({@code java} or {@code java-library}) to apply and configure the {@code java},
 * {@code javadoc}, {@code test}, and {@code test-retry} plugins.
 */
class JavaConventions {

    static final JavaVersion SOURCE_COMPATIBILITY = JavaVersion.VERSION_11;

    static final JavaVersion TARGET_COMPATIBILITY = JavaVersion.VERSION_1_8;

    void apply(Project project) {
        project.getPlugins().withType(JavaBasePlugin.class, (javaPlugin) -> {
            configureJavaCompileConventions(project);
            configureJavaConventions(project);
            configureJarConventions(project);
            configureJavadocConventions(project);
            configureTestConventions(project);
        });
        project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> configureOptionalFeatureConventions(project));
    }

    private void configureJavaCompileConventions(Project project) {
        project.getTasks().withType(JavaCompile.class, (compileTask) -> {
            compileTask.getOptions().setEncoding("UTF-8");
            compileTask.getOptions().getRelease().set(Integer.valueOf(SOURCE_COMPATIBILITY.getMajorVersion()));
            compileTask.setSourceCompatibility(SOURCE_COMPATIBILITY.toString());
            compileTask.setTargetCompatibility(TARGET_COMPATIBILITY.toString());
            List<String> args = compileTask.getOptions().getCompilerArgs();
            args.addAll(Arrays.asList("-Xlint:unchecked", "-Xlint:deprecation"));
        });
    }

    private void configureJavaConventions(Project project) {
        project.getExtensions().configure(JavaPluginExtension.class, (java) -> {
            java.setSourceCompatibility(SOURCE_COMPATIBILITY);
            java.setTargetCompatibility(TARGET_COMPATIBILITY);
            java.toolchain((toolchain) -> toolchain.getLanguageVersion().set(javaLanguageVersion()));
        });
    }

    private void configureJarConventions(Project project) {
        ExtractResources extractLegalResources = project.getTasks().create("extractLegalResources",
                ExtractResources.class);
        extractLegalResources.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("legal"));
        extractLegalResources.setResourcesNames(Arrays.asList("LICENSE", "NOTICE"));
        project.getTasks().withType(Jar.class, (jarTask) -> project.afterEvaluate((evaluated) -> {
            jarTask.metaInf((metaInf) -> {
                metaInf.from(extractLegalResources);
                metaInf.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            });
            jarTask.manifest((manifest) -> {
                Map<String, Object> attributes = Collections.singletonMap("Automatic-Module-Name",
                        project.getName().replace("new-relic", "newrelic").replace("-", "."));
                manifest.attributes(attributes);
            });
        }));
    }

    private void configureJavadocConventions(Project project) {
        project.getTasks().withType(Javadoc.class, (javadocTask) -> {
            StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadocTask.getOptions();
            options.source(SOURCE_COMPATIBILITY.toString());
            options.encoding("UTF-8");
            options.addBooleanOption("Xdoclint:all,-missing", true);
            options.tags("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:",
                    "implNote:a:Implementation Note:");
        });
    }

    private void configureTestConventions(Project project) {
        project.getTasks().withType(Test.class, (testTask) -> {
            testTask.useJUnitPlatform((options) -> options.excludeTags("docker"));
            testTask.setMaxHeapSize("1500m");
            testTask.testLogging((options) -> options.setExceptionFormat(TestExceptionFormat.FULL));
            project.getTasks().withType(Checkstyle.class, testTask::mustRunAfter);
            project.getTasks().withType(CheckFormat.class, testTask::mustRunAfter);
            TestRetryExtension testRetry = testTask.getExtensions().getByType(TestRetryExtension.class);
            testRetry.getMaxRetries().set(3);
            testRetry.getMaxFailures().set(5);
        });
        project.getTasks().create("dockerTest", Test.class,
                (dockerTestTask) -> dockerTestTask.useJUnitPlatform((options) -> options.includeTags("docker")));
        project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> project.getDependencies()
                .add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher"));
    }

    private void configureOptionalFeatureConventions(Project project) {
        project.getExtensions().configure(JavaPluginExtension.class, (java) -> {
            SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            java.registerFeature("optional", (optionalFeature) -> optionalFeature.usingSourceSet(mainSourceSet));
        });
    }

    private JavaVersion javaVersion() {
        return (JavaVersion.current().isJava11Compatible()) ? JavaVersion.current() : JavaVersion.VERSION_17;
    }

    private JavaLanguageVersion javaLanguageVersion() {
        return JavaLanguageVersion.of(javaVersion().toString());
    }

}
