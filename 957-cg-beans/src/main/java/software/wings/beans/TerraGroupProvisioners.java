/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
