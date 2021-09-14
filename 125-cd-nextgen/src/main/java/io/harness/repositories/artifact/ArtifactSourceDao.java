/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.artifact;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource.ArtifactSourceKeys;
import io.harness.validation.PersistenceValidator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Entries are immutable, thus no update function should be there.
 */
@Singleton
public class ArtifactSourceDao {
  @Inject private ArtifactRepository artifactRepository;

  public ArtifactSource create(ArtifactSource artifactSource) {
    return PersistenceValidator.duplicateCheck(
        () -> artifactRepository.save(artifactSource), ArtifactSourceKeys.uniqueHash, artifactSource.getUniqueHash());
  }

  public ArtifactSource get(String accountId, String uuid) {
    return artifactRepository.findByAccountIdAndUuid(accountId, uuid);
  }

  public ArtifactSource getArtifactStreamByHash(String accountId, String uniqueHash) {
    return artifactRepository.findByAccountIdAndUniqueHash(accountId, uniqueHash);
  }
}
