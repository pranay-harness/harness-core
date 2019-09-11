package io.harness.generator.artifactstream;

import com.google.inject.Inject;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream.AmiArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;

import javax.validation.constraints.NotNull;

public class AmazonAmiArtifactStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    return ensureArtifactStream(seed,
        AmiArtifactStream.builder()
            .name("aws-playground-ami")
            .appId(application.getAppId())
            .serviceId(service.getUuid())
            .settingId(settingAttribute.getUuid())
            .region("us-east-1")
            .sourceName("us-east-1")
            .build(),
        owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, @NotNull ArtifactStream artifactStream, Owners owners) {
    AmiArtifactStream amiArtifactStream = (AmiArtifactStream) artifactStream;
    final AmiArtifactStreamBuilder amiArtifactStreamBuilder = AmiArtifactStream.builder();

    if (artifactStream != null && artifactStream.getAppId() != null) {
      amiArtifactStreamBuilder.appId(amiArtifactStream.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getServiceId() != null) {
      amiArtifactStreamBuilder.serviceId(amiArtifactStream.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getName() != null) {
      amiArtifactStreamBuilder.name(artifactStream.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSourceName() != null) {
      amiArtifactStreamBuilder.sourceName(amiArtifactStream.getSourceName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (amiArtifactStream.getRegion() != null) {
      amiArtifactStreamBuilder.region(amiArtifactStream.getRegion());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSettingId() != null) {
      amiArtifactStreamBuilder.settingId(amiArtifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }

    ArtifactStream existingArtifactStream = artifactStreamGeneratorHelper.exists(amiArtifactStreamBuilder.build());
    if (existingArtifactStream != null) {
      return existingArtifactStream;
    }

    return artifactStreamGeneratorHelper.saveArtifactStream(amiArtifactStreamBuilder.build(), owners);
  }
}
