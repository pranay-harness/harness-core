/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
