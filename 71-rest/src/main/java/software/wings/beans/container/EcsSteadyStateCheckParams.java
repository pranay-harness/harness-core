package software.wings.beans.container;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsSteadyStateCheckParams implements ExecutionCapabilityDemander {
  private String appId;
  private String region;
  private String accountId;
  private long timeoutInMs;
  private String activityId;
  private String commandName;
  private String clusterName;
  private String serviceName;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(awsConfig, encryptionDetails);
  }
}
