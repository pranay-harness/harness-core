package io.harness.generator.artifactstream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;

@Singleton
public class ArtifactStreamGeneratorHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ArtifactStreamService artifactStreamService;

  public ArtifactStream exists(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(ArtifactStream.class)
        .filter(ArtifactStream.APP_ID_KEY, artifactStream.getAppId())
        .filter(ArtifactStream.SERVICE_ID_KEY, artifactStream.getServiceId())
        .filter(ArtifactStream.NAME_KEY, artifactStream.getName())
        .get();
  }

  public ArtifactStream saveArtifactStream(ArtifactStream artifactStream) {
    try {
      return artifactStreamService.create(artifactStream, false);
    } catch (DuplicateKeyException de) {
      ArtifactStream exists = exists(artifactStream);
      if (exists != null) {
        return exists;
      }
      throw de;
    }
  }
}
