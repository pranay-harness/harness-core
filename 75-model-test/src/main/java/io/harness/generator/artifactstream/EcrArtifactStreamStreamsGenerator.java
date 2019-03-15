package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;

@Singleton
public class EcrArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifact(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    String serviceId = service.getUuid();

    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    ArtifactStream artifactStream = EcrArtifactStream.builder()
                                        .appId(application.getUuid())
                                        .serviceId(serviceId)
                                        .region("us-east-1")
                                        .imageName("hello-world")
                                        .name("hello-world")
                                        .autoPopulate(true)
                                        .settingId(settingAttribute.getUuid())
                                        .build();
    return ensureArtifactStream(seed, artifactStream);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, ArtifactStream artifactStream) {
    EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
    ArtifactStream existing = artifactStreamGeneratorHelper.exists(ecrArtifactStream);
    if (existing != null) {
      return existing;
    }
    return artifactStreamGeneratorHelper.saveArtifactStream(EcrArtifactStream.builder()
                                                                .appId(ecrArtifactStream.getAppId())
                                                                .serviceId(ecrArtifactStream.getServiceId())
                                                                .name(ecrArtifactStream.getName())
                                                                .region(ecrArtifactStream.getRegion())
                                                                .imageName(ecrArtifactStream.getImageName())
                                                                .autoPopulate(ecrArtifactStream.isAutoPopulate())
                                                                .settingId(ecrArtifactStream.getSettingId())
                                                                .build());
  }
}
