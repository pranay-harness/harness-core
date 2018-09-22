package software.wings.sm.states;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KubernetesSwapServiceSelectorsResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
}
