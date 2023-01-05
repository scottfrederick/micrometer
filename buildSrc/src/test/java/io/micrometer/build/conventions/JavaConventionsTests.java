/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this sourcesJar except in compliance with the License.
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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

class JavaConventionsTests {

    private File projectDir;

    private File buildFile;

    @BeforeEach
    void setup(@TempDir File projectDir) throws IOException {
        this.projectDir = projectDir;
        this.buildFile = new File(this.projectDir, "build.gradle");
        File settingsFile = new File(this.projectDir, "settings.gradle");
        try (PrintWriter out = new PrintWriter(new FileWriter(settingsFile))) {
            out.println("plugins {");
            out.println("    id 'com.gradle.enterprise'");
            out.println("}");
        }
        File licenseFile = new File(this.projectDir, "LICENSE");
        try (PrintWriter out = new PrintWriter(new FileWriter(licenseFile))) {
            out.println("license info");
        }
        File noticeFile = new File(this.projectDir, "NOTICE");
        try (PrintWriter out = new PrintWriter(new FileWriter(noticeFile))) {
            out.println("notice");
        }
    }

    @Test
    void jarIsBuilt() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'java-library'");
            out.println("    id 'io.micrometer.publish'");
            out.println("    id 'io.micrometer.conventions'");
            out.println("}");
            out.println("group = 'io.micrometer.test'");
            out.println("version = '1.2.3'");
        }
        runGradle("jar");
        File file = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3.jar");
        assertThat(file).exists();
        try (JarFile jar = new JarFile(file)) {
            assertLegalFilesPresent(jar);
            assertManifestEntriesPopulated(jar, this.projectDir.getName());
        }
    }

    @Test
    void sourcesAndJavadocJarsAreBuiltWhenPublishPluginApplied() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'java-library'");
            out.println("    id 'io.micrometer.publish'");
            out.println("    id 'io.micrometer.conventions'");
            out.println("}");
            out.println("group = 'io.micrometer.test'");
            out.println("version = '1.2.3'");
        }
        runGradle("assemble");
        File sourcesJar = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-sources.jar");
        assertThat(sourcesJar).exists();
        try (JarFile jar = new JarFile(sourcesJar)) {
            assertLegalFilesPresent(jar);
            assertManifestEntriesPopulated(jar, this.projectDir.getName());
        }
        File javadocJar = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-javadoc.jar");
        assertThat(javadocJar).exists();
        try (JarFile jar = new JarFile(javadocJar)) {
            assertLegalFilesPresent(jar);
            assertManifestEntriesPopulated(jar, this.projectDir.getName());
        }
    }

    @Test
    void sourcesAndJavadocJarsAreNotBuiltWhenPublishPluginNotApplied() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'java-library'");
            out.println("    id 'io.micrometer.conventions'");
            out.println("}");
            out.println("version = '1.2.3'");
        }
        runGradle("assemble");
        File sourcesJar = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-sources.jar");
        assertThat(sourcesJar).doesNotExist();
        File javadocJar = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3-javadoc.jar");
        assertThat(javadocJar).doesNotExist();
    }

    private void assertManifestEntriesPopulated(JarFile jar, String projectName) throws IOException {
        Attributes mainAttributes = jar.getManifest().getMainAttributes();
        assertThat(mainAttributes.getValue("Automatic-Module-Name"))
                .isEqualTo(this.projectDir.getName().replace("-", "."));
        assertThat(mainAttributes.getValue("Implementation-Title"))
                .isEqualTo("io.micrometer.test#" + projectName + ";1.2.3");
        assertThat(mainAttributes.getValue("Implementation-Version")).isEqualTo("1.2.3");
    }

    private void assertLegalFilesPresent(JarFile jar) {
        assertThat(jar.getJarEntry("META-INF/LICENSE")).isNotNull();
        assertThat(jar.getJarEntry("META-INF/NOTICE")).isNotNull();
    }

    private BuildResult runGradle(String... args) {
        return runGradle(Collections.emptyMap(), args);
    }

    private BuildResult runGradle(Map<String, String> environment, String... args) {
        return GradleRunner.create().withProjectDir(this.projectDir).withEnvironment(environment).withArguments(args)
                .withPluginClasspath().build();
    }

}
