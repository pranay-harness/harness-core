package io.harness.delegate.beans.executioncapability;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

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
}
