/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface TerraGroupProvisioners {
  void setTemplatized(boolean isTemplatized);
  void setNormalizedPath(String normalizedPath);
  boolean isTemplatized();
  String getNormalizedPath();
  String getSourceRepoBranch();
  String getSourceRepoSettingId();
  String getPath();
  String getRepoName();
}
