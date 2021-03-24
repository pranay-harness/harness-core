package software.wings.beans.governance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.governance.TimeRangeBasedFreezeConfigYaml;

import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class GovernanceConfigYaml extends BaseEntityYaml {
  private boolean disableAllDeployments;
  private List<TimeRangeBasedFreezeConfigYaml> timeRangeBasedFreezeConfigs;

  @lombok.Builder
  public GovernanceConfigYaml(String type, String harnessApiVersion, boolean disableAllDeployments,
      List<TimeRangeBasedFreezeConfigYaml> timeRangeBasedFreezeConfigs) {
    super(type, harnessApiVersion);
    this.disableAllDeployments = disableAllDeployments;
    this.timeRangeBasedFreezeConfigs = timeRangeBasedFreezeConfigs;
  }
}
