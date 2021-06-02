package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitValidationParameters implements ExecutionCapabilityDemander {
  GitConfig gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  private boolean isGitHostConnectivityCheck;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (isGitHostConnectivityCheck) {
      return CapabilityHelper.generateExecutionCapabilitiesForGit(gitConfig);
    } else {
      return Collections.singletonList(GitConnectionCapability.builder()
                                           .gitConfig(gitConfig)
                                           .settingAttribute(gitConfig.getSshSettingAttribute())
                                           .encryptedDataDetails(encryptedDataDetails)
                                           .build());
    }
  }
}
