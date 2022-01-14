package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.RepairActionCode;

import software.wings.yaml.BaseYamlWithType;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@TargetModule(HarnessModule._957_CG_BEANS)
public class RuntimeInputsConfig {
  List<String> runtimeInputVariables;
  long timeout;
  List<String> userGroupIds;
  RepairActionCode timeoutAction;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYamlWithType {
    List<String> runtimeInputVariables;
    long timeout;
    List<String> userGroupNames;
    RepairActionCode timeoutAction;

    @Builder
    public Yaml(
        List<String> runtimeInputVariables, long timeout, List<String> userGroupNames, RepairActionCode timeoutAction) {
      this.runtimeInputVariables = runtimeInputVariables;
      this.timeout = timeout;
      this.userGroupNames = userGroupNames;
      this.timeoutAction = timeoutAction;
    }
  }
}
