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

package io.micrometer.build.publish;

import nebula.plugin.contacts.ContactsPlugin;
import nebula.plugin.info.InfoPlugin;
import nebula.plugin.publishing.maven.MavenDeveloperPlugin;
import nebula.plugin.publishing.maven.MavenManifestPlugin;
import nebula.plugin.publishing.maven.MavenNebulaPublishPlugin;
import nebula.plugin.publishing.maven.license.MavenApacheLicensePlugin;
import nebula.plugin.publishing.publications.JavadocJarPlugin;
import nebula.plugin.publishing.publications.SourceJarPlugin;
import nebula.plugin.publishing.verification.PublishVerificationPlugin;
import nebula.plugin.responsible.NebulaResponsiblePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugins.signing.SigningPlugin;

public class PublishPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        configurePublishPlugins(project);
    }

    private void configurePublishPlugins(Project project) {
        project.getPlugins().apply(SigningPlugin.class);
        project.getPlugins().apply(MavenNebulaPublishPlugin.class);
        project.getPlugins().apply(MavenManifestPlugin.class);
        project.getPlugins().apply(MavenDeveloperPlugin.class);
        project.getPlugins().apply(JavadocJarPlugin.class);
        project.getPlugins().apply(SourceJarPlugin.class);
        project.getPlugins().apply(MavenApacheLicensePlugin.class);
        project.getPlugins().apply(PublishVerificationPlugin.class);
        project.getPlugins().apply(ContactsPlugin.class);
        project.getPlugins().apply(InfoPlugin.class);
        project.getPlugins().apply(NebulaResponsiblePlugin.class);
    }

}
