package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.RepairActionCode;

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
public class RuntimeInputsConfig {
  List<String> runtimeInputVariables;
  long timeout;
  List<String> userGroupIds;
  RepairActionCode timeoutAction;
}
