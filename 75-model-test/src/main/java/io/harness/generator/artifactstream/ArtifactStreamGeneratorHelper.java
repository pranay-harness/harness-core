package io.harness.generator.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

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
    // TODO: ASR: IMP: update this after refactor
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, artifactStream.fetchAppId())
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
    if (!savedArtifactStream.getAppId().equals(GLOBAL_APP_ID)) {
      Service service = owners.obtainService();
      if (service == null) {
        return savedArtifactStream;
      }

      // TODO: ASR: update this method after refactor

      List<String> artifactStreamIds = service.getArtifactStreamIds();
      if (artifactStreamIds == null) {
        artifactStreamIds = new ArrayList<>();
      }
      if (!artifactStreamIds.contains(savedArtifactStream.getUuid())) {
        artifactStreamIds.add(savedArtifactStream.getUuid());
        Service updatedService = serviceResourceService.updateArtifactStreamIds(service, artifactStreamIds);
        owners.obtainService().setArtifactStreamIds(updatedService.getArtifactStreamIds());
      }
    }
    return savedArtifactStream;
  }
}
