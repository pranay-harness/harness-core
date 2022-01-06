/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.fullsync.dtos.GitFullSyncEntityInfoDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncEntityInfoFilterDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;

@OwnedBy(DX)
public interface GitFullSyncEntityService {
  GitFullSyncEntityInfo save(GitFullSyncEntityInfo gitFullSyncEntityInfo);

  void markQueuedOrFailed(String uuid, String accountId, long currentRetryCount, long maxRetryCount, String errorMsg);

  void markSuccessful(String uuid, String accountId);

  PageResponse<GitFullSyncEntityInfoDTO> list(String account, String org, String project, PageRequest pageRequest,
      String searchTerm, GitFullSyncEntityInfoFilterDTO giFullSyncEntityInfoFilterDTO);

  long count(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      GitFullSyncEntityInfoFilterDTO gitFullSyncEntityInfoFilterDTO);

  List<GitFullSyncEntityInfo> list(String accountIdentifier, String fullSyncJobId);
}
