/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.fullSync;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(PL)
public interface FullSyncJobRepository extends CrudRepository<GitFullSyncJob, String>, FullSyncJobRepositoryCustom {
  GitFullSyncJob findByAccountIdentifierAndUuid(String accountIdentifier, String uuid);
}
