package software.wings.generator;

import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

public class ArtifactStreamGenerator {
  @Inject ArtifactStreamService artifactStreamService;

  public ArtifactStream createArtifactStream(long seed, ArtifactStream artifactStream) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    ArtifactStreamType artifactStreamType;

    if (artifactStream != null && artifactStream.getArtifactStreamType() != null) {
      artifactStreamType = ArtifactStreamType.valueOf(artifactStream.getArtifactStreamType());
    } else {
      artifactStreamType = random.nextObject(ArtifactStreamType.class);
    }

    ArtifactStream newArtifactStream = null;
    switch (artifactStreamType) {
      case JENKINS:
        JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStream;
        final JenkinsArtifactStream.Builder builder = aJenkinsArtifactStream();

        if (jenkinsArtifactStream != null && jenkinsArtifactStream.getJobname() != null) {
          builder.withJobname(jenkinsArtifactStream.getJobname());
        } else {
          throw new UnsupportedOperationException();
        }

        if (jenkinsArtifactStream != null && jenkinsArtifactStream.getArtifactPaths() != null) {
          builder.withArtifactPaths(jenkinsArtifactStream.getArtifactPaths());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getAppId() != null) {
          builder.withAppId(artifactStream.getAppId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getServiceId() != null) {
          builder.withServiceId(artifactStream.getServiceId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getSourceName() != null) {
          builder.withSourceName(artifactStream.getSourceName());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getSettingId() != null) {
          builder.withSettingId(artifactStream.getSettingId());
        } else {
          throw new UnsupportedOperationException();
        }

        newArtifactStream = builder.build();
        break;
      default:
        throw new UnsupportedOperationException();
    }

    return artifactStreamService.create(newArtifactStream);
  }
}
