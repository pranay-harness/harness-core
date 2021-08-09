package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDP)
@TargetModule(HarnessModule._959_CG_BEANS)
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
