package software.wings.beans;

import io.harness.interrupts.RepairActionCode;

import software.wings.yaml.BaseYamlWithType;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class RuntimeInputsConfigYaml extends BaseYamlWithType {
  List<String> runtimeInputVariables;
  long timeout;
  List<String> userGroupNames;
  RepairActionCode timeoutAction;

  @Builder
  public RuntimeInputsConfigYaml(
      List<String> runtimeInputVariables, long timeout, List<String> userGroupNames, RepairActionCode timeoutAction) {
    this.runtimeInputVariables = runtimeInputVariables;
    this.timeout = timeout;
    this.userGroupNames = userGroupNames;
    this.timeoutAction = timeoutAction;
  }
}
