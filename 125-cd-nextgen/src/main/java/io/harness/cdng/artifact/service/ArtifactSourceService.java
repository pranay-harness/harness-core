/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.artifact.service;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.validation.Create;

import javax.validation.Valid;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Entries are immutable, thus no update function should be there.
 */
public interface ArtifactSourceService {
  /**
   * Create/Save ArtifactSource in collection.
   * @param artifactSource
   * @return ArtifactSource
   */
  @ValidationGroups(Create.class) ArtifactSource create(@Valid ArtifactSource artifactSource);

  /**
   * Create/Save ArtifactSource in collection with validations.
   * @param artifactSource
   * @param validate
   * @return ArtifactSource
   */
  @ValidationGroups(Create.class) ArtifactSource create(@Valid ArtifactSource artifactSource, boolean validate);

  /**
   * Save if not created and return artifactSource.
   * @param artifactSource
   * @return ArtifactSource.
   */
  @ValidationGroups(Create.class) ArtifactSource saveOrGetArtifactStream(@Valid ArtifactSource artifactSource);

  /**
   * Get ArtifactSource using accountId, uuid.
   * @param accountId
   * @param uuid
   * @return ArtifactSource
   */
  ArtifactSource get(String accountId, String uuid);

  /**
   * Get ArtifactSource based on accountId, uniqueHash(set by different stream types).
   * @param accountId
   * @param uniqueHash
   * @return artifactStream
   */
  ArtifactSource getArtifactStreamByHash(String accountId, String uniqueHash);
}
