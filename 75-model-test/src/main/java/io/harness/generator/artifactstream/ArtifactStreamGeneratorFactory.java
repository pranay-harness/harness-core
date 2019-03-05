package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;

@Singleton
public class ArtifactStreamGeneratorFactory {
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private EcrArtifactStreamStreamsGenerator ecrArtifactStreamGenerator;
  @Inject private JenkinsArtifactStreamStreamsGenerator jenkinsArtifactStreamStreamsGenerator;
  @Inject private AmazonS3ArtifactStreamStreamsGenerator amazonS3ArtifactStreamStreamsGenerator;
  @Inject private ArtifactoryArtifactStreamStreamsGenerator artifactoryArtifactStreamStreamsGenerator;

  public ArtifactStreamsGenerator getArtifactStreamGenerator(ArtifactStreams artifactStreams) {
    if (ArtifactStreams.HARNESS_SAMPLE_ECR.equals(artifactStreams)) {
      return ecrArtifactStreamGenerator;
    } else if (ArtifactStreams.ARTIFACTORY_ECHO_WAR.equals(artifactStreams)) {
      return artifactoryArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_IIS_APP.equals(artifactStreams)) {
      return amazonS3ArtifactStreamStreamsGenerator;
    }
    if (ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR.equals(artifactStreams)) {
      return jenkinsArtifactStreamStreamsGenerator;
    }
    return null;
  }
}
