package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.ExecutionInterrupt;

import java.util.Date;

@OwnedBy(CDC)
@Value
@Builder
public class StateExecutionInterrupt {
  ExecutionInterrupt interrupt;
  Date tookAffectAt;
}
