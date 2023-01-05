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

import de.undercouch.gradle.tasks.download.Download;
import de.undercouch.gradle.tasks.download.org.apache.hc.core5.net.URIBuilder;
import me.champeau.gradle.japicmp.JapicmpPlugin;
import me.champeau.gradle.japicmp.JapicmpTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.bundling.Jar;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Conventions that are applied in the presence of a {@link JapicmpPlugin} plugin to apply
 * and configure java archive compare tasks.
 */
class JarCompareConventions {

    public static final String SKIP_COMPATIBILITY_CHECK = "SKIP";

    void apply(Project project) {
        project.getPlugins().withType(JapicmpPlugin.class, (japicmpPlugin) -> {
            Download download = configureDownload(project);
            configureJavaArchiveCompare(project, download);
        });
    }

    private Download configureDownload(Project project) {
        return project.getTasks().create("downloadBaseline", Download.class, (downloadTask) -> {
            Object compatibleVersion = getCompatibleVersion(project);
            downloadTask.onlyIf((element) -> {
                if (project.getGradle().getStartParameter().isOffline()) {
                    System.out.println("Offline: skipping downloading and performing baseline comparison");
                    return false;
                }
                else if (SKIP_COMPATIBILITY_CHECK.equals(compatibleVersion)) {
                    System.out.println("SKIP: skipping downloading and performing baseline comparison");
                    return false;
                }
                else {
                    System.out.println("Downloading and performing baseline comparison with " + compatibleVersion);
                    return true;
                }
            });
            downloadTask.onlyIfNewer(true);
            downloadTask.compress(true);
            downloadTask.src(getDownloadSourceUrl(project));
            downloadTask.dest(getBaselineJarFilePath(project).toFile());
        });
    }

    private void configureJavaArchiveCompare(Project project, Download downloadTask) {
        project.getTasks().create("japicmp", JapicmpTask.class, (japicmpTask) -> {
            japicmpTask.onlyIf((element) -> !SKIP_COMPATIBILITY_CHECK.equals(getCompatibleVersion(project)));
            japicmpTask.getOldClasspath().from(getBaselineJarFilePath(project));
            project.getTasks().withType(Jar.class,
                    (jarTask) -> japicmpTask.getNewClasspath().from(jarTask.getArchiveFile()));
            japicmpTask.getTxtOutputFile()
                    .set(Paths.get(project.getBuildDir().getPath(), "reports", "japi.txt").toFile());
            japicmpTask.getOnlyBinaryIncompatibleModified().set(true);
            japicmpTask.getFailOnModification().set(true);
            japicmpTask.getFailOnSourceIncompatibility().set(true);
            japicmpTask.getIgnoreMissingClasses().set(true);
            japicmpTask.getIncludeSynthetic().set(true);
            japicmpTask.getCompatibilityChangeExcludes().add("METHOD_NEW_DEFAULT");
            japicmpTask.getPackageExcludes().addAll("io.micrometer.shaded.*", "io.micrometer.statsd.internal");
            japicmpTask.dependsOn(downloadTask);
            project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(japicmpTask);
        });
    }

    private URL getDownloadSourceUrl(Project project) {
        try {
            String repositoryRootUrl = getRepositoryRootUrl(project);
            return new URIBuilder(repositoryRootUrl)
                    .appendPathSegments("io", "micrometer", project.getName(), getCompatibleVersion(project).toString(),
                            project.getName() + "-" + getCompatibleVersion(project) + ".jar")
                    .build().toURL();
        }
        catch (URISyntaxException | MalformedURLException ex) {
            throw new GradleException("Error building download URL", ex);
        }
    }

    private String getRepositoryRootUrl(Project project) {
        String compatibleVersion = getCompatibleVersion(project).toString();
        if (compatibleVersion.contains("-M") || compatibleVersion.contains("-RC1")) {
            return "https://repo.spring.io/milestone";
        }
        if (compatibleVersion.contains("-SNAPSHOT")) {
            return "https://repo.spring.io/snapshot";
        }
        String rootUrl = project.getRepositories().mavenCentral().getUrl().toString();
        if (rootUrl.endsWith("/")) {
            return rootUrl.substring(0, rootUrl.length() - 1);
        }
        return rootUrl;
    }

    private Path getBaselineJarFilePath(Project project) {
        return Paths.get(project.getBuildDir().getPath(), "baselineLibs",
                project.getName() + "-" + getCompatibleVersion(project) + ".jar");
    }

    private Object getCompatibleVersion(Project project) {
        return project.findProperty("compatibleVersion");
    }

}
