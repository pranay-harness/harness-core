package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.JenkinsArtifactStream.JenkinsArtifactStreamBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.SettingGenerator.Settings;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGenerator {
  @Inject ArtifactStreamService artifactStreamService;

  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;

  @Inject WingsPersistence wingsPersistence;

  public enum ArtifactStreams {
    HARNESS_SAMPLE_ECHO_WAR,
  }

  public ArtifactStream ensurePredefined(Randomizer.Seed seed, Owners owners, ArtifactStreams predefined) {
    switch (predefined) {
      case HARNESS_SAMPLE_ECHO_WAR:
        return ensureHarnessSampleEchoWar(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private ArtifactStream ensureHarnessSampleEchoWar(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    Service service = owners.obtainService();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_JENKINS_CONNECTOR);

    return ensureArtifactStream(seed,
        JenkinsArtifactStream.builder()
            .appId(environment.getAppId())
            .serviceId(service.getUuid())
            .name("harness-samples")
            .sourceName(settingAttribute.getName())
            .jobname("harness-samples")
            .artifactPaths(asList("echo/target/echo.war"))
            .settingId(settingAttribute.getUuid())
            .build());
  }

  public ArtifactStream ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);

    ArtifactStreams predefined = random.nextObject(ArtifactStreams.class);

    return ensurePredefined(seed, owners, predefined);
  }

  public ArtifactStream exists(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, artifactStream.getAppId())
        .filter(ArtifactStream.SERVICE_ID_KEY, artifactStream.getServiceId())
        .filter(ArtifactStream.NAME_KEY, artifactStream.getName())
        .get();
  }

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

        if (artifactStream != null && artifactStream.getName() != null) {
          builder.name(artifactStream.getName());
        } else {
          throw new UnsupportedOperationException();
        }

        ArtifactStream existing = exists(builder.build());
        if (existing != null) {
          return existing;
        }

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

    return artifactStreamService.forceCreate(newArtifactStream);
  }
}
