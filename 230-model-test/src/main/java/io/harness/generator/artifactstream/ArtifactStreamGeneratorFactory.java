/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ArtifactStreamGeneratorFactory {
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private EcrArtifactStreamStreamsGenerator ecrArtifactStreamGenerator;
  @Inject private AzureArtifactStreamsGenerator azureArtifactStreamGenerator;
  @Inject private DockerArtifactStreamStreamsGenerator dockerArtifactStreamGenerator;
  @Inject private JenkinsArtifactStreamStreamsGenerator jenkinsArtifactStreamStreamsGenerator;
  @Inject private AmazonS3ArtifactStreamStreamsGenerator amazonS3ArtifactStreamStreamsGenerator;
  @Inject private ArtifactoryArtifactStreamStreamsGenerator artifactoryArtifactStreamStreamsGenerator;
  @Inject private AmazonAmiArtifactStreamsGenerator amazonAmiArtifactStreamsGenerator;
  @Inject
  private AzureMachineImageLinuxGalleryArtifactStreamGenerator azureMachineImageLinuxGalleryArtifactStreamGenerator;
  @Inject private AmazonLambdaArtifactStreamGenerator amazonLambdaArtifactStreamGenerator;
  @Inject private SpotinstAmiArtifactStreamsGenerator spotinstAmiArtifactStreamsGenerator;
  @Inject private BambooArtifactStreamGenerator bambooArtifactStreamGenerator;
  @Inject private Nexus2MavenArtifactStreamsGenerator nexus2MavenArtifactStreamsGenerator;
  @Inject private Nexus3MavenArtifactStreamsGenerator nexus3MavenArtifactStreamsGenerator;
  @Inject private Nexus3NpmArtifactStreamsGenerator nexus3NpmArtifactStreamsGenerator;
  @Inject private Nexus3DockerArtifactStreamsGenerator nexus3DockerArtifactStreamsGenerator;

  public ArtifactStreamsGenerator getArtifactStreamGenerator(ArtifactStreams artifactStreams) {
    if (ArtifactStreams.HARNESS_SAMPLE_ECR == artifactStreams) {
      return ecrArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_DOCKER == artifactStreams) {
      return dockerArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_AZURE == artifactStreams) {
      return azureArtifactStreamGenerator;
    }
    if (ArtifactStreams.ARTIFACTORY_ECHO_WAR == artifactStreams || ArtifactStreams.PCF == artifactStreams) {
      return artifactoryArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_IIS_APP_S3 == artifactStreams) {
      return amazonS3ArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR == artifactStreams) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.AWS_AMI == artifactStreams) {
      return amazonAmiArtifactStreamsGenerator;
    }
    if (ArtifactStreams.SPOTINST_AMI == artifactStreams) {
      return spotinstAmiArtifactStreamsGenerator;
    }
    if (ArtifactStreams.AZURE_MACHINE_IMAGE_LINUX_GALLERY == artifactStreams) {
      return azureMachineImageLinuxGalleryArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_EXAMPLE_LAMBDA == artifactStreams) {
      return amazonLambdaArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR_AT_CONNECTOR == artifactStreams) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_DOCKER_AT_CONNECTOR == artifactStreams) {
      return dockerArtifactStreamGenerator;
    }
    if (ArtifactStreams.JENKINS_METADATA_ONLY == artifactStreams) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.BAMBOO_METADATA_ONLY == artifactStreams) {
      return bambooArtifactStreamGenerator;
    }
    if (ArtifactStreams.NEXUS2_MAVEN_METADATA_ONLY == artifactStreams
        || ArtifactStreams.NEXUS2_MAVEN_METADATA_ONLY_PARAMETERIZED == artifactStreams
        || ArtifactStreams.NEXUS2_NPM_METADATA_ONLY_PARAMETERIZED == artifactStreams
        || ArtifactStreams.NEXUS2_NUGET_METADATA_ONLY_PARAMETERIZED == artifactStreams) {
      return nexus2MavenArtifactStreamsGenerator;
    }
    if (ArtifactStreams.NEXUS3_NPM_METADATA_ONLY == artifactStreams) {
      return nexus3MavenArtifactStreamsGenerator;
    }
    if (ArtifactStreams.NEXUS3_DOCKER_METADATA_ONLY == artifactStreams) {
      return nexus3DockerArtifactStreamsGenerator;
    }
    if (ArtifactStreams.NEXUS3_MAVEN_METADATA_ONLY == artifactStreams) {
      return nexus3MavenArtifactStreamsGenerator;
    }

    throw new InvalidRequestException(
        "Artifact stream generator not supported for " + artifactStreams, WingsException.USER);
  }
}
