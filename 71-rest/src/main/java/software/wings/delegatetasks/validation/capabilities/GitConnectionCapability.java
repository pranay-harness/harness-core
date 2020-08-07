package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;

import java.util.List;

/**
 * Adding Setting attribute here is wrong but people used a variety of git URL's and we need to have an appropriate
 * parser to extract the hosts and check connectivity.
 *
 * Following the old model here.
 */
@Value
@Builder
public class GitConnectionCapability implements ExecutionCapability {
  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }
  GitConfig gitConfig;
  @ToString.Exclude SettingAttribute settingAttribute;
  List<EncryptedDataDetail> encryptedDataDetails;
  CapabilityType capabilityType = CapabilityType.GIT_CONNECTION;

  @Override
  public String fetchCapabilityBasis() {
    return "GIT:" + gitConfig.getRepoUrl();
  }
}
