package io.harness.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.JenkinsArtifactStream.JenkinsArtifactStreamBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.SettingGenerator.Settings;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream.AmazonS3ArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.ArtifactoryArtifactStreamBuilder;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGenerator {
  @Inject ArtifactStreamService artifactStreamService;

  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;

  @Inject WingsPersistence wingsPersistence;

  public enum ArtifactStreams { HARNESS_SAMPLE_ECHO_WAR }

  public ArtifactStream ensurePredefined(Randomizer.Seed seed, Owners owners, ArtifactStreams predefined) {
    switch (predefined) {
      case HARNESS_SAMPLE_ECHO_WAR:
        return ensureHarnessSampleEchoWar(seed, owners, null);
      default:
        unhandled(predefined);
    }

    return null;
  }

  public ArtifactStream ensureHarnessSampleEchoWar(Randomizer.Seed seed, Owners owners, String serviceId) {
    Environment environment =
        owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));
    if (serviceId == null) {
      Service service = owners.obtainService();
      serviceId = service.getUuid();
    }

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_JENKINS_CONNECTOR);

    return ensureArtifactStream(seed,
        JenkinsArtifactStream.builder()
            .appId(environment.getAppId())
            .serviceId(serviceId)
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

    ArtifactStream newArtifactStream;
    switch (artifactStreamType) {
      case JENKINS:
        JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStream;
        final JenkinsArtifactStreamBuilder builder = JenkinsArtifactStream.builder();

        builder.appId(artifactStream.getAppId());

        builder.serviceId(artifactStream.getServiceId());

        builder.name(artifactStream.getName());

        ArtifactStream existing = exists(builder.build());
        if (existing != null) {
          return existing;
        }
        if (jenkinsArtifactStream.getJobname() != null) {
          builder.jobname(jenkinsArtifactStream.getJobname());
        } else {
          throw new UnsupportedOperationException();
        }

        if (jenkinsArtifactStream.getArtifactPaths() != null) {
          builder.artifactPaths(jenkinsArtifactStream.getArtifactPaths());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getSourceName() != null) {
          builder.sourceName(artifactStream.getSourceName());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getSettingId() != null) {
          builder.settingId(artifactStream.getSettingId());
        } else {
          throw new UnsupportedOperationException();
        }

        newArtifactStream = builder.build();
        break;

      case AMAZON_S3:
        AmazonS3ArtifactStream amazonS3ArtifactStream = (AmazonS3ArtifactStream) artifactStream;
        final AmazonS3ArtifactStreamBuilder s3ArtifactStreamBuilder = AmazonS3ArtifactStream.builder();

        if (artifactStream != null && artifactStream.getAppId() != null) {
          s3ArtifactStreamBuilder.appId(artifactStream.getAppId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getServiceId() != null) {
          s3ArtifactStreamBuilder.serviceId(artifactStream.getServiceId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getName() != null) {
          s3ArtifactStreamBuilder.name(artifactStream.getName());
        } else {
          throw new UnsupportedOperationException();
        }

        ArtifactStream existingArtifactStream = exists(s3ArtifactStreamBuilder.build());
        if (existingArtifactStream != null) {
          return existingArtifactStream;
        }

        if (amazonS3ArtifactStream.getJobname() != null) {
          s3ArtifactStreamBuilder.jobname(amazonS3ArtifactStream.getJobname());
        } else {
          throw new UnsupportedOperationException();
        }

        if (amazonS3ArtifactStream.getArtifactPaths() != null) {
          s3ArtifactStreamBuilder.artifactPaths(amazonS3ArtifactStream.getArtifactPaths());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getSourceName() != null) {
          s3ArtifactStreamBuilder.sourceName(artifactStream.getSourceName());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getSettingId() != null) {
          s3ArtifactStreamBuilder.settingId(artifactStream.getSettingId());
        } else {
          throw new UnsupportedOperationException();
        }

        newArtifactStream = s3ArtifactStreamBuilder.build();
        break;
      case ARTIFACTORY:
        ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
        final ArtifactoryArtifactStreamBuilder artifactoryArtifactStreamBuilder = ArtifactoryArtifactStream.builder();

        if (artifactStream != null && artifactStream.getAppId() != null) {
          artifactoryArtifactStreamBuilder.appId(artifactStream.getAppId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getServiceId() != null) {
          artifactoryArtifactStreamBuilder.serviceId(artifactStream.getServiceId());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getName() != null) {
          artifactoryArtifactStreamBuilder.name(artifactStream.getName());
        } else {
          throw new UnsupportedOperationException();
        }

        ArtifactStream existingArtifactoryArtifactStream = exists(artifactoryArtifactStreamBuilder.build());
        if (existingArtifactoryArtifactStream != null) {
          return existingArtifactoryArtifactStream;
        }

        if (artifactoryArtifactStream.getJobname() != null) {
          artifactoryArtifactStreamBuilder.jobname(artifactoryArtifactStream.getJobname());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactoryArtifactStream.getArtifactPaths() != null) {
          artifactoryArtifactStreamBuilder.artifactPaths(artifactoryArtifactStream.getArtifactPaths());
        }

        if (artifactStream.getSourceName() != null) {
          artifactoryArtifactStreamBuilder.sourceName(artifactStream.getSourceName());
        } else {
          throw new UnsupportedOperationException();
        }

        if (artifactStream.getSettingId() != null) {
          artifactoryArtifactStreamBuilder.settingId(artifactStream.getSettingId());
        } else {
          throw new UnsupportedOperationException();
        }

        artifactoryArtifactStreamBuilder.metadataOnly(artifactStream.isMetadataOnly());

        newArtifactStream = artifactoryArtifactStreamBuilder.build();
        break;
      default:
        throw new UnsupportedOperationException();
    }
    return artifactStreamService.forceCreate(newArtifactStream);
  }
}
