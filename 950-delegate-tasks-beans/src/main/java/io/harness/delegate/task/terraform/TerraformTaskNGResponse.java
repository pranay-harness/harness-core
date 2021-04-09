package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedRecordData;

import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(CDP)
public class TerraformTaskNGResponse implements DelegateResponseData {
  CommandExecutionStatus commandExecutionStatus;
  String errorMessage;
  @NonFinal @Setter UnitProgressData unitProgressData;

  Map<String, String> commitIdForConfigFilesMap;
  EncryptedRecordData encryptedTfPlan;
}
