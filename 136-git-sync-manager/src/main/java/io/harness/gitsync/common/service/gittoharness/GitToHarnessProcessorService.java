/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
      String branch, String repoUrl, String commitId, String gitToHarnessProgressRecordId, String changeSetId);
}
