package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

@Data
@Builder
public class KubernetesSwapServiceSelectorsResponse implements NotifyResponseData {
  private ExecutionStatus executionStatus;
}
