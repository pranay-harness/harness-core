package io.harness.delegate.beans.executioncapability;

import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
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

  GitConfigDTO gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  CapabilityType capabilityType = CapabilityType.GIT_CONNECTION_NG;

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
