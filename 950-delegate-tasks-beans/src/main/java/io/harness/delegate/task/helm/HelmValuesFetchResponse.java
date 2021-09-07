package io.harness.delegate.task.helm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class HelmValuesFetchResponse implements DelegateTaskNotifyResponseData {
  private CommandExecutionStatus commandExecutionStatus;
  private String errorMessage;
  private UnitProgressData unitProgressData;
  private String valuesFileContent;
  @NonFinal @Setter private DelegateMetaInfo delegateMetaInfo;
}
