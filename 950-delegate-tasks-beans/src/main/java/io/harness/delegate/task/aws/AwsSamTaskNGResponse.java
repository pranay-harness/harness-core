package io.harness.delegate.task.aws;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedRecordData;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Value
@Builder
@OwnedBy(CDP)
public class AwsSamTaskNGResponse implements DelegateTaskNotifyResponseData {
    CommandExecutionStatus commandExecutionStatus;
    String errorMessage;
    @NonFinal
    @Setter
    UnitProgressData unitProgressData;

    Map<String, String> commitIdForConfigFilesMap;
    EncryptedRecordData encryptedTfPlan;
    String outputs;
    String stateFileId;
    @NonFinal @Setter
    DelegateMetaInfo delegateMetaInfo;
}
