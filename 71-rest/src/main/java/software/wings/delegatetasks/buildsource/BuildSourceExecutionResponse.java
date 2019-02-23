package software.wings.delegatetasks.buildsource;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BuildSourceExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private BuildSourceResponse buildSourceResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
