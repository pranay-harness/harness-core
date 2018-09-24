package software.wings.helpers.ext.helm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HelmCommandExecutionResponse extends DelegateTaskNotifyResponseData {
  private HelmCommandResponse helmCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
