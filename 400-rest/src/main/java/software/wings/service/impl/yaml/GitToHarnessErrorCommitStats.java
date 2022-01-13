/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml;

import software.wings.beans.GitRepositoryInfo;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "GitToHarnessErrorCommitStatsKeys")
public class GitToHarnessErrorCommitStats {
  String gitCommitId;
  Integer failedCount;
  Long commitTime;
  String gitConnectorId;
  String branchName;
  String repositoryName;
  String gitConnectorName;
  String commitMessage;
  List<GitSyncError> errorsForSummaryView;
  GitRepositoryInfo repositoryInfo;
}
