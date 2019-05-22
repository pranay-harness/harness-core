package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.GeneratorUtils;
import io.harness.generator.OwnerManager.Owners;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ArtifactStreamGeneratorHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject ServiceResourceService serviceResourceService;

  // TODO: ASR: update this method to use setting_id + name after refactoring
  public ArtifactStream exists(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, artifactStream.getAppId())
        .filter(ArtifactStreamKeys.serviceId, artifactStream.getServiceId())
        .filter(ArtifactStreamKeys.name, artifactStream.getName())
        .get();
  }

  public ArtifactStream saveArtifactStream(ArtifactStream artifactStream, Owners owners) {
    return GeneratorUtils.suppressDuplicateException(
        () -> createArtifactStream(artifactStream, owners), () -> exists(artifactStream));
  }

  private ArtifactStream createArtifactStream(ArtifactStream artifactStream, Owners owners) {
    ArtifactStream savedArtifactStream = artifactStreamService.create(artifactStream, false);
    Service service = owners.obtainService();
    if (service == null) {
      return savedArtifactStream;
    }

    List<String> artifactStreamIds = service.getArtifactStreamIds();
    if (artifactStreamIds == null) {
      artifactStreamIds = new ArrayList<>();
    }
    if (!artifactStreamIds.contains(savedArtifactStream.getUuid())) {
      artifactStreamIds.add(savedArtifactStream.getUuid());
      serviceResourceService.updateArtifactStreamIds(service, artifactStreamIds);
    }

    return savedArtifactStream;
  }
}
