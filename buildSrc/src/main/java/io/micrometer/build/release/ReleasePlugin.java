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

package io.micrometer.build.release;

import java.net.URI;

import io.github.gradlenexus.publishplugin.NexusPublishExtension;
import io.github.gradlenexus.publishplugin.NexusPublishPlugin;
import nebula.plugin.release.git.base.ReleasePluginExtension;
import nebula.plugin.release.git.opinion.Strategies;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ReleasePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        configureReleasePlugins(project);
        configureNexusPlugin(project);
    }

    private void configureReleasePlugins(Project project) {
        project.getPlugins().apply(nebula.plugin.release.ReleasePlugin.class);
        ReleasePluginExtension release = project.getExtensions().getByType(ReleasePluginExtension.class);
        release.setDefaultVersionStrategy(Strategies.getSNAPSHOT());
    }

    private void configureNexusPlugin(Project project) {
        project.getPlugins().apply(NexusPublishPlugin.class);
        NexusPublishExtension nexusPublish = project.getExtensions().getByType(NexusPublishExtension.class);
        nexusPublish.repositories((repositories) -> repositories.create("mavenCentral", (repository) -> {
            repository.getNexusUrl().set(URI.create("https://s01.oss.sonatype.org/service/local/"));
            repository.getSnapshotRepositoryUrl().set(URI.create("https://repo.spring.io/snapshot/"));
            repository.getUsername().set(String.valueOf(project.findProperty("MAVEN_CENTRAL_USER")));
            repository.getPassword().set(String.valueOf(project.findProperty("MAVEN_CENTRAL_PASSWORD")));
        }));
    }

}
