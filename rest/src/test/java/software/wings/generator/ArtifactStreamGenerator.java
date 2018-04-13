package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.JenkinsArtifactStream.JenkinsArtifactStreamBuilder;
import static software.wings.beans.artifact.JenkinsArtifactStream.builder;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGenerator {
  @Inject ApplicationGenerator applicationGenerator;

  @Inject ArtifactStreamService artifactStreamService;

  public enum ArtifactStreams {
    JENKINS_TEST,
  }

  public ArtifactStream ensurePredefined(long seed, ArtifactStreams predefined) {
    switch (predefined) {
      case JENKINS_TEST:
        return ensureJenkinsTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private ArtifactStream ensureJenkinsTest(long seed) {
    return ensureArtifactStream(seed,
        JenkinsArtifactStream.builder()
            .sourceName("todolistwar")
            .settingId(SETTING_ID)
            .appId(APP_ID)
            .jobname("todolistwar")
            .autoPopulate(true)
            .serviceId(SERVICE_ID)
            .artifactPaths(asList("target/todolist.war"))
            .build());
  }

  public ArtifactStream ensureArtifactStream(long seed, ArtifactStream artifactStream) {
    EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).build();

    ArtifactStreamType artifactStreamType;

    if (artifactStream != null && artifactStream.getArtifactStreamType() != null) {
      artifactStreamType = ArtifactStreamType.valueOf(artifactStream.getArtifactStreamType());
    } else {
      artifactStreamType = random.nextObject(ArtifactStreamType.class);
    }

    ArtifactStream newArtifactStream;
    switch (artifactStreamType) {
      case JENKINS:
        JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStream;
        JenkinsArtifactStreamBuilder builder = builder();

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
          Application application = applicationGenerator.ensureRandom(seed);
          builder.appId(application.getAppId());
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

        builder.autoPopulate(artifactStream.isAutoPopulate());

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
