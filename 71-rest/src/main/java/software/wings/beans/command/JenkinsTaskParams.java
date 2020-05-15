package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Data
@Builder
public class JenkinsTaskParams implements ExecutionCapabilityDemander {
  private JenkinsConfig jenkinsConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String jobName;
  private Map<String, String> parameters;
  private Map<String, String> filePathsForAssertion;
  private String activityId;
  private String unitName;
  private boolean unstableSuccess;
  private boolean injectEnvVars;
  private JenkinsSubTaskType subTaskType;
  private String queuedBuildUrl;
  private long timeout;
  private long startTs;
  private String appId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    // Ideally we should check for capability for getting encryption details
    // but the original validation task does not do that
    return jenkinsConfig.fetchRequiredExecutionCapabilities();
  }
}
