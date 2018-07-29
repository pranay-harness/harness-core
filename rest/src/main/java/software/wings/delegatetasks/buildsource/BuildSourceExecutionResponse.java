package software.wings.delegatetasks.buildsource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BuildSourceExecutionResponse extends DelegateTaskNotifyResponseData {
  private BuildSourceResponse buildSourceDelegateResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
