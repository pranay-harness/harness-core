/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(PL)
public interface GitSyncErrorRepository extends CrudRepository<GitSyncError, String>, GitSyncErrorRepositoryCustom {
  GitSyncError findByAccountIdentifierAndCompleteFilePathAndErrorType(
      String accountId, String yamlFilePath, GitSyncErrorType errorType);
}
