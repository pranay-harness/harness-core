package io.harness.cdng.artifact.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource.ArtifactSourceKeys;
import io.harness.persistence.HPersistence;
import io.harness.validation.PersistenceValidator;

/**
 * Entries are immutable, thus no update function should be there.
 */
@Singleton
public class ArtifactSourceDao {
  @Inject private HPersistence hPersistence;

  public ArtifactSource create(ArtifactSource artifactSource) {
    String id = PersistenceValidator.duplicateCheck(
        () -> hPersistence.save(artifactSource), ArtifactSourceKeys.uniqueHash, artifactSource.getUniqueHash());
    return get(artifactSource.getAccountId(), id);
  }

  public ArtifactSource get(String accountId, String uuid) {
    return hPersistence.createQuery(ArtifactSource.class)
        .filter(ArtifactSourceKeys.accountId, accountId)
        .filter(ArtifactSourceKeys.uuid, uuid)
        .get();
  }

  public ArtifactSource getArtifactStreamByHash(String accountId, String uniqueHash) {
    return hPersistence.createQuery(ArtifactSource.class)
        .filter(ArtifactSourceKeys.accountId, accountId)
        .filter(ArtifactSourceKeys.uniqueHash, uniqueHash)
        .get();
  }
}
