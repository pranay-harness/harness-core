/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;

@OwnedBy(DX)
public interface GitSyncableEntity extends NGAccess {
  String getUuid();

  String getObjectIdOfYaml();

  void setObjectIdOfYaml(String objectIdOfYaml);

  Boolean getIsFromDefaultBranch();

  void setIsFromDefaultBranch(Boolean isFromDefaultBranch);

  void setBranch(String branch);

  String getBranch();

  String getYamlGitConfigRef();

  void setYamlGitConfigRef(String yamlGitConfigRef);

  String getRootFolder();

  void setRootFolder(String rootFolder);

  String getFilePath();

  void setFilePath(String filePath);
}
