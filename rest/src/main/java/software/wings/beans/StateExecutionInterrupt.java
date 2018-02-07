package software.wings.beans;

import lombok.Builder;
import lombok.Value;
import software.wings.sm.ExecutionInterrupt;

import java.util.Date;

@Builder
@Value
public class StateExecutionInterrupt {
  ExecutionInterrupt interrupt;
  Date tookAffectAt;
}
