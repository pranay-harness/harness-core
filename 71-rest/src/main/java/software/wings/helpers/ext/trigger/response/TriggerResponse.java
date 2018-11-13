package software.wings.helpers.ext.trigger.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerResponse extends DelegateTaskNotifyResponseData {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
