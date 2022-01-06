/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.service.gittoharness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public interface GitToHarnessProcessorService {
  GitToHarnessProgressStatus processFiles(String accountId, List<GitToHarnessFileProcessingRequest> filesToBeProcessed,
      String branch, String repoUrl, String commitId, String gitToHarnessProgressRecordId, String changeSetId,
      String commitMessage);
}
