package software.wings.helpers.ext.helm;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@Builder
public class HelmCommandExecutionResponse implements NotifyResponseData {
  private HelmCommandResponse helmCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
