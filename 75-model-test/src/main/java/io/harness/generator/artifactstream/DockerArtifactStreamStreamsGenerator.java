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
import software.wings.beans.artifact.DockerArtifactStream;

@Singleton
public class DockerArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifact(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    String serviceId = service.getUuid();

    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_DOCKER_REGISTRY);

    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .appId(application.getUuid())
                                        .name("nginx")
                                        .serviceId(serviceId)
                                        .settingId(settingAttribute.getUuid())
                                        .imageName("library/nginx")
                                        .build();
    return ensureArtifactStream(seed, artifactStream);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, ArtifactStream artifactStream) {
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
                                                                .build());
  }
}
