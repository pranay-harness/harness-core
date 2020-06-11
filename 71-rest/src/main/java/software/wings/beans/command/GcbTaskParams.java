package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GcpConfig;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Data
@Builder
public class GcbTaskParams implements ExecutionCapabilityDemander {
  private GcpConfig gcpConfig;
  private String projectId;
  private String triggerId; // rework this into a list of options
  private String branchName; // rework this into a list of options
  private List<EncryptedDataDetail> encryptedDataDetails;
  private Map<String, String> parameters;
  private String activityId;
  private String unitName;
  private boolean injectEnvVars;
  private String buildUrl;
  private long timeout;
  private long startTs;
  private String appId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return gcpConfig.fetchRequiredExecutionCapabilities();
  }
}
