package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.GeneratorUtils;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGeneratorHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ArtifactStreamService artifactStreamService;

  public ArtifactStream exists(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, artifactStream.getAppId())
        .filter(ArtifactStreamKeys.serviceId, artifactStream.getServiceId())
        .filter(ArtifactStreamKeys.name, artifactStream.getName())
        .get();
  }

  public ArtifactStream saveArtifactStream(ArtifactStream artifactStream) {
    return GeneratorUtils.suppressDuplicateException(
        () -> artifactStreamService.create(artifactStream, false), () -> exists(artifactStream));
  }
}
