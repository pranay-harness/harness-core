package software.wings.sm.states;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KubernetesSwapServiceSelectorsResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
}
