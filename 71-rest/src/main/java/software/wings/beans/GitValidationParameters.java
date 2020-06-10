package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class GitValidationParameters implements ExecutionCapabilityDemander {
  GitConfig gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(GitConnectionCapability.builder()
                                         .gitConfig(gitConfig)
                                         .settingAttribute(gitConfig.getSshSettingAttribute())
                                         .encryptedDataDetails(encryptedDataDetails)
                                         .build());
  }
}
