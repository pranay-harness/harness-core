package io.harness.generator.artifactstream;

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
  public ArtifactStream ensureArtifact(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_ARTIFACTORY_CONNECTOR);

    ArtifactStream artifactStream = ArtifactoryArtifactStream.builder()
                                        .appId(application.getUuid())
                                        .serviceId(service.getUuid())
                                        .name("artifactory-echo-war")
                                        .jobname("functional-test")
                                        .autoPopulate(true)
                                        .artifactPattern("/io/harness/e2e/echo/*/*.war")
                                        .settingId(settingAttribute.getUuid())
                                        .build();
    return ensureArtifactStream(seed, artifactStream);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream) {
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

    return artifactStreamGeneratorHelper.saveArtifactStream(artifactoryArtifactStreamBuilder.build());
  }
}
