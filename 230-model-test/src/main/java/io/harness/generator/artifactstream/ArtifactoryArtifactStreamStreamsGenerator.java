package io.harness.generator.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream.ArtifactoryArtifactStreamBuilder;

@Singleton
public class ArtifactoryArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, atConnector, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_ARTIFACTORY_CONNECTOR);

    ArtifactStream artifactStream =
        ArtifactoryArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : service != null ? service.getUuid() : null)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "artifactory-echo-war-metadataOnly" : "artifactory-echo-war")
            .jobname("functional-test")
            .autoPopulate(true)
            .artifactPattern("/io/harness/e2e/echo/*/*.war")
            .settingId(settingAttribute.getUuid())
            .build();
    return ensureArtifactStream(seed, artifactStream, owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream, Owners owners) {
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

    ArtifactStream existingArtifactoryArtifactStream =
        artifactStreamGeneratorHelper.exists(artifactoryArtifactStreamBuilder.build());
    if (existingArtifactoryArtifactStream != null) {
      return existingArtifactoryArtifactStream;
    }

    if (artifactoryArtifactStream.getJobname() != null) {
      artifactoryArtifactStreamBuilder.jobname(artifactoryArtifactStream.getJobname());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactoryArtifactStream.getArtifactPattern() != null) {
      artifactoryArtifactStreamBuilder.artifactPattern(artifactoryArtifactStream.getArtifactPattern());
    }

    if (artifactStream.getSettingId() != null) {
      artifactoryArtifactStreamBuilder.settingId(artifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }
    artifactoryArtifactStreamBuilder.autoPopulate(artifactoryArtifactStream.isAutoPopulate());
    artifactoryArtifactStreamBuilder.metadataOnly(artifactStream.isMetadataOnly());

    return artifactStreamGeneratorHelper.saveArtifactStream(artifactoryArtifactStreamBuilder.build(), owners);
  }
}
