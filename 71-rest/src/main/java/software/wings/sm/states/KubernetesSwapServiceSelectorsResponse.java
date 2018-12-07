package software.wings.sm.states;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KubernetesSwapServiceSelectorsResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
}
