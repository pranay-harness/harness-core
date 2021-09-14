/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.artifact;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ArtifactRepository extends PagingAndSortingRepository<ArtifactSource, String> {
  ArtifactSource findByAccountIdAndUniqueHash(String accountId, String uniqueHash);
  ArtifactSource findByAccountIdAndUuid(String accountId, String uuid);
}
