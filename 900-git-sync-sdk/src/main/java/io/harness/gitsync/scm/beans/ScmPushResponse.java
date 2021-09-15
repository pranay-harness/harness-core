/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.scm.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.DX)
@Getter
@SuperBuilder
public abstract class ScmPushResponse {
  String filePath;
  boolean pushToDefaultBranch;
  String yamlGitConfigId;
  String objectId;
  String folderPath;
  String branch;
  String commitId;
}
