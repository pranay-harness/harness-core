package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.RepairActionCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
