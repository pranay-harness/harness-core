package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CfCommandExecutionResponse implements DelegateTaskNotifyResponseData {
  private CfCommandResponse pcfCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
  private DelegateMetaInfo delegateMetaInfo;
}
