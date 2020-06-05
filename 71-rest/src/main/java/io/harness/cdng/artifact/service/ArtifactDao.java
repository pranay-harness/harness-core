package io.harness.cdng.artifact.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.Artifact;
import io.harness.cdng.artifact.bean.Artifact.ArtifactsNGKeys;
import software.wings.dl.WingsPersistence;

/**
 * Entries are immutable, thus no update function should be there.
 */
@Singleton
public class ArtifactDao {
  @Inject private WingsPersistence wingsPersistence;

  public Artifact create(Artifact artifact) {
    String id = wingsPersistence.save(artifact);
    return get(artifact.getAccountId(), id);
  }

  public Artifact get(String accountId, String uuid) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter(ArtifactsNGKeys.accountId, accountId)
        .filter(ArtifactsNGKeys.uuid, uuid)
        .get();
  }
}
