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

  public ArtifactStreamsGenerator getArtifactStreamGenerator(ArtifactStreams artifactStreams) {
    if (ArtifactStreams.HARNESS_SAMPLE_ECR.equals(artifactStreams)) {
      return ecrArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_DOCKER.equals(artifactStreams)) {
      return dockerArtifactStreamGenerator;
    }
    if (ArtifactStreams.ARTIFACTORY_ECHO_WAR.equals(artifactStreams) || ArtifactStreams.PCF.equals(artifactStreams)) {
      return artifactoryArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_IIS_APP.equals(artifactStreams)) {
      return amazonS3ArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR.equals(artifactStreams)) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.AWS_AMI.equals(artifactStreams)) {
      return amazonAmiArtifactStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_EXAMPLE_LAMBDA.equals(artifactStreams)) {
      return amazonLambdaArtifactStreamGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR_AT_CONNECTOR.equals(artifactStreams)) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_DOCKER_AT_CONNECTOR.equals(artifactStreams)) {
      return dockerArtifactStreamGenerator;
    }

    throw new InvalidRequestException(
        "Artifact stream generator not supported for " + artifactStreams, WingsException.USER);
  }
}
