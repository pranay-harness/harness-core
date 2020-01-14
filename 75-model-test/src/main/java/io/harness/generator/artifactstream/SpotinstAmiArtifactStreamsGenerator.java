package io.harness.generator.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream.AmiArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;

import javax.validation.constraints.NotNull;

public class SpotinstAmiArtifactStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;
  @Inject private SettingGenerator settingGenerator;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    Service service = owners.obtainService();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.AWS_SPOTINST_TEST_CLOUD_PROVIDER);

    String serviceId = service != null ? service.getUuid() : null;

    return ensureArtifactStream(seed,
        AmiArtifactStream.builder()
            .name("aws-spotinst-ami")
            .appId(atConnector ? GLOBAL_APP_ID : owners.obtainApplication().getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : serviceId)
            .settingId(settingAttribute.getUuid())
            .region("us-east-1")
            .sourceName("us-east-1")
            .autoPopulate(false)
            .build(),
        owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, @NotNull ArtifactStream artifactStream, Owners owners) {
    AmiArtifactStream amiArtifactStream = (AmiArtifactStream) artifactStream;
    final AmiArtifactStreamBuilder amiArtifactStreamBuilder = AmiArtifactStream.builder();

    Preconditions.checkNotNull(artifactStream.getAppId());
    Preconditions.checkNotNull(artifactStream.getServiceId());
    Preconditions.checkNotNull(artifactStream.getName());
    Preconditions.checkNotNull(artifactStream.getSourceName());
    Preconditions.checkNotNull(amiArtifactStream.getRegion());
    Preconditions.checkNotNull(amiArtifactStream.getSettingId());

    amiArtifactStreamBuilder.appId(amiArtifactStream.getAppId());
    amiArtifactStreamBuilder.serviceId(amiArtifactStream.getServiceId());
    amiArtifactStreamBuilder.name(artifactStream.getName());
    amiArtifactStreamBuilder.sourceName(amiArtifactStream.getSourceName());
    amiArtifactStreamBuilder.region(amiArtifactStream.getRegion());
    amiArtifactStreamBuilder.settingId(amiArtifactStream.getSettingId());

    ArtifactStream existingArtifactStream = artifactStreamGeneratorHelper.exists(amiArtifactStreamBuilder.build());
    if (existingArtifactStream != null) {
      return existingArtifactStream;
    }

    return artifactStreamGeneratorHelper.saveArtifactStream(amiArtifactStreamBuilder.build(), owners);
  }
}
