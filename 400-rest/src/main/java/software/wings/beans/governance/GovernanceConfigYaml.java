package software.wings.beans.governance;

import io.harness.governance.TimeRangeBasedFreezeConfigYaml;

import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
