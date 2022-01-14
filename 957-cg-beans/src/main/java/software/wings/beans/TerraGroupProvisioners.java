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
