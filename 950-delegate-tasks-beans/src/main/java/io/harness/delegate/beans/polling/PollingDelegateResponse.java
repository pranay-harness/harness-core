package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class PollingDelegateResponse {
  private String accountId;
  private String pollingDocId;
  private PollingResponseInfc pollingResponseInfc;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
