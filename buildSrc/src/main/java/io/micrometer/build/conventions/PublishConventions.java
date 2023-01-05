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

import groovy.lang.MissingPropertyException;
import groovy.util.Node;
import nebula.plugin.contacts.Contact;
import nebula.plugin.contacts.ContactsExtension;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.plugins.signing.SigningExtension;

/**
 * Conventions that are applied in the presence of a {@link MavenPublishPlugin} plugin to
 * apply and configure Nebula release and publish tasks.
 */
class PublishConventions {

    void apply(Project project) {
        project.getPlugins().withType(MavenPublishPlugin.class).all((mavenPublish) -> {
            project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
                configurePublishingConventions(project);
                configureLibraryPublishingContent(project);
            });
            project.getPlugins().withType(JavaPlatformPlugin.class, (javaPlatformPlugin) -> {
                configurePublishingConventions(project);
                configurePlatformPublishingContent(project);
            });
        });
    }

    private void configurePublishingConventions(Project project) {
        configureContacts(project);
        disableModuleMetadata(project);
        configurePomMetadataWarnings(project);
        configureRepositories(project);
        configureSigning(project);
    }

    private void configureContacts(Project project) {
        ContactsExtension contacts = project.getExtensions().getByType(ContactsExtension.class);
        addContact(contacts, "tludwig@vmware.com", "Tommy Ludwig", "shakuzen");
    }

    private void addContact(ContactsExtension contacts, String email, String moniker, String github) {
        Contact contact = (Contact) contacts.addPerson(email);
        contact.setMoniker(moniker);
        contact.setGithub(github);
    }

    private void disableModuleMetadata(Project project) {
        // Nebula doesn't interface with Gradle's module format so just disable it for
        // now.
        TaskCollection<GenerateModuleMetadata> generateModuleMetadataTasks = project.getTasks()
                .withType(GenerateModuleMetadata.class);
        generateModuleMetadataTasks.forEach((task) -> task.setEnabled(false));
    }

    private void configurePomMetadataWarnings(Project project) {
        configureMavenPublications(project, MavenPublication::suppressAllPomMetadataWarnings);
    }

    private void configureRepositories(Project project) {
        configureMavenPublishing(project, (publishing) -> {
            publishing.repositories((repositories) -> {
                repositories.maven((maven) -> {
                    maven.setName("Snapshot");
                    maven.setUrl("https://repo.spring.io/snapshot");
                    maven.credentials((credentials) -> {
                        credentials.setUsername(String.valueOf(project.findProperty("SNAPSHOT_REPO_USER")));
                        credentials.setPassword(String.valueOf(project.findProperty("SNAPSHOT_REPO_PASSWORD")));
                    });
                });
                repositories.maven((maven) -> {
                    maven.setName("Milestone");
                    maven.setUrl("https://repo.spring.io/milestone");
                    maven.credentials((credentials) -> {
                        credentials.setUsername(String.valueOf(project.findProperty("MILESTONE_REPO_USER")));
                        credentials.setPassword(String.valueOf(project.findProperty("MILESTONE_REPO_PASSWORD")));
                    });
                });
            });
        });
    }

    private void configureSigning(Project project) {
        project.getPlugins().withId("maven-publish", (mavenPublishPlugin) -> {
            String circleStage = System.getenv("CIRCLE_STAGE");
            if (circleStage != null && circleStage.equals("deploy")) {
                try {
                    SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);
                    signing.setRequired(true);
                    signing.useInMemoryPgpKeys(String.valueOf(project.property("SIGNING_KEY")),
                            String.valueOf(project.property("SIGNING_PASSWORD'")));
                    configureMavenPublications(project, signing::sign);
                }
                catch (MissingPropertyException ex) {
                    throw new GradleException("Properties 'SIGNING_KEY' and 'SIGNING_PASSWORD' must be set "
                            + "to enable artifact signing before publishing");
                }
            }
        });
    }

    private void configureLibraryPublishingContent(Project project) {
        configureMavenPublications(project, (publication) -> {
            publication.versionMapping(
                    (strategy) -> strategy.allVariants(VariantVersionMappingStrategy::fromResolutionResult));
            publication.pom((pom) -> {
                // We publish resolved versions so don't need to publish our
                // dependencyManagement too. This is different from many Maven
                // projects, where published artifacts often don't include resolved
                // versions and have a parent POM including dependencyManagement.
                pom.withXml((xml) -> {
                    Object dependencyManagementNode = xml.asNode().get("dependencyManagement");
                    if (dependencyManagementNode != null) {
                        xml.asNode().remove((Node) dependencyManagementNode);
                    }
                });
            });
        });
    }

    private void configurePlatformPublishingContent(Project project) {
        configureMavenPublications(project, (publication) -> {
            project.getComponents().matching((component) -> component.getName().equals("javaPlatform"))
                    .all(publication::from);
        });
    }

    private void configureMavenPublications(Project project, Action<? super MavenPublication> configure) {
        configureMavenPublishing(project, (publishing) -> publishing.publications((publications) -> {
            publications.withType(MavenPublication.class).all(configure);
        }));
    }

    private void configureMavenPublishing(Project project, Action<? super PublishingExtension> configure) {
        project.getPlugins().withId("maven-publish", (mavenPublishPlugin) -> {
            PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
            configure.execute(publishing);
        });
    }

}
