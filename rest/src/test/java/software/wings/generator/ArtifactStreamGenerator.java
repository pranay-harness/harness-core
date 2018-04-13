package software.wings.generator;

import static software.wings.beans.artifact.JenkinsArtifactStream.JenkinsArtifactStreamBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGenerator {
  @Inject ArtifactStreamService artifactStreamService;

  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream) {
    EnhancedRandom random = Randomizer.instance(seed);

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
        final JenkinsArtifactStreamBuilder builder = JenkinsArtifactStream.builder();

        if (jenkinsArtifactStream != null && jenkinsArtifactStream.getJobname() != null) {
          builder.jobname(jenkinsArtifactStream.getJobname());
        } else {
          throw new UnsupportedOperationException();
        }

        if (jenkinsArtifactStream != null && jenkinsArtifactStream.getArtifactPaths() != null) {
          builder.artifactPaths(jenkinsArtifactStream.getArtifactPaths());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getAppId() != null) {
          builder.appId(artifactStream.getAppId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getServiceId() != null) {
          builder.serviceId(artifactStream.getServiceId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getSourceName() != null) {
          builder.sourceName(artifactStream.getSourceName());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream != null && artifactStream.getSettingId() != null) {
          builder.settingId(artifactStream.getSettingId());
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
