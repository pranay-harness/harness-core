package io.harness.generator.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

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
import software.wings.beans.artifact.DockerArtifactStream;

@Singleton
public class DockerArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_DOCKER_REGISTRY);

    ArtifactStream artifactStream =
        DockerArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .name(atConnector ? "nginx-atConnector" : "nginx")
            .serviceId(atConnector ? settingAttribute.getUuid() : service != null ? service.getUuid() : null)
            .settingId(settingAttribute.getUuid())
            .imageName("library/nginx")
            .build();
    return ensureArtifactStream(seed, artifactStream, owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, ArtifactStream artifactStream, Owners owners) {
    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
    ArtifactStream existing = artifactStreamGeneratorHelper.exists(dockerArtifactStream);
    if (existing != null) {
      return existing;
    }
    return artifactStreamGeneratorHelper.saveArtifactStream(DockerArtifactStream.builder()
                                                                .appId(dockerArtifactStream.getAppId())
                                                                .serviceId(dockerArtifactStream.getServiceId())
                                                                .name(dockerArtifactStream.getName())
                                                                .imageName(dockerArtifactStream.getImageName())
                                                                .autoPopulate(dockerArtifactStream.isAutoPopulate())
                                                                .settingId(dockerArtifactStream.getSettingId())
                                                                .build(),
        owners);
  }
}
