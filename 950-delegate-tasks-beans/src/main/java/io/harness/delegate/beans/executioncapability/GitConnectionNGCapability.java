package io.harness.delegate.beans.executioncapability;

import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitConnectionNGCapability implements ExecutionCapability {
  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  GitConfig gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  CapabilityType capabilityType = CapabilityType.GIT_CONNECTION_NG;
  SSHKeySpecDTO sshKeySpecDTO;

  @Override
  public String fetchCapabilityBasis() {
    return "GIT: " + gitConfig.getUrl();
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
