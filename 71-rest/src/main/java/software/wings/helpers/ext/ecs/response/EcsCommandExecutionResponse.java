package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsCommandExecutionResponse implements ResponseData, DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private EcsCommandResponse ecsCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}