package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;

import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(CDP)
public class AwsSamTaskNGResponse implements DelegateTaskNotifyResponseData {
  CommandExecutionStatus commandExecutionStatus;
  String errorMessage;
  @NonFinal @Setter UnitProgressData unitProgressData;

  Map<String, String> commitIdForConfigFilesMap;
  String outputs;
  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
}
