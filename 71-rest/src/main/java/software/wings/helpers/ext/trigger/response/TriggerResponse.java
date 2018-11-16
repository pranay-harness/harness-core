package software.wings.helpers.ext.trigger.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
  private String errorMsg;
}
