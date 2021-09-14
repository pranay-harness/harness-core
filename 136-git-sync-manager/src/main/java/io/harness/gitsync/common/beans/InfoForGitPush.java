/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class InfoForGitPush {
  ScmConnector scmConnector;
  String folderPath;
  String filePath;
  String branch;
  boolean isNewBranch;
  String baseBranch;
  String accountId;
  String projectIdentifier;
  String orgIdentifier;
  String commitMsg;
  String oldFileSha;
  String yaml;
}
