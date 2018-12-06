package software.wings.helpers.ext.trigger.response;

import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
  private String errorMsg;
}
