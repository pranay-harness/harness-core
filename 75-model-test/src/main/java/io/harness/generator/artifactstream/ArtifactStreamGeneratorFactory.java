package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;

@Singleton
public class ArtifactStreamGeneratorFactory {
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private EcrArtifactStreamStreamsGenerator ecrArtifactStreamGenerator;
  @Inject private DockerArtifactStreamStreamsGenerator dockerArtifactStreamGenerator;
  @Inject private JenkinsArtifactStreamStreamsGenerator jenkinsArtifactStreamStreamsGenerator;
  @Inject private AmazonS3ArtifactStreamStreamsGenerator amazonS3ArtifactStreamStreamsGenerator;
  @Inject private ArtifactoryArtifactStreamStreamsGenerator artifactoryArtifactStreamStreamsGenerator;
  @Inject private AmazonAmiArtifactStreamsGenerator amazonAmiArtifactStreamsGenerator;
  @Inject private AmazonLambdaArtifactStreamGenerator amazonLambdaArtifactStreamGenerator;
  @Inject private SpotinstAmiArtifactStreamsGenerator spotinstAmiArtifactStreamsGenerator;
  @Inject private BambooArtifactStreamGenerator bambooArtifactStreamGenerator;
  @Inject private Nexus2MavenArtifactStreamsGenerator nexus2MavenArtifactStreamsGenerator;
  @Inject private Nexus3MavenArtifactStreamsGenerator nexus3MavenArtifactStreamsGenerator;

  public ArtifactStreamsGenerator getArtifactStreamGenerator(ArtifactStreams artifactStreams) {
    if (ArtifactStreams.HARNESS_SAMPLE_ECR == artifactStreams) {
      return ecrArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_DOCKER == artifactStreams) {
      return dockerArtifactStreamGenerator;
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
    if (ArtifactStreams.NEXUS2_MAVEN_METADATA_ONLY == artifactStreams) {
      return nexus2MavenArtifactStreamsGenerator;
    }
    if (ArtifactStreams.NEXUS3_MAVEN_METADATA_ONLY == artifactStreams) {
      return nexus3MavenArtifactStreamsGenerator;
    }

    throw new InvalidRequestException(
        "Artifact stream generator not supported for " + artifactStreams, WingsException.USER);
  }
}
