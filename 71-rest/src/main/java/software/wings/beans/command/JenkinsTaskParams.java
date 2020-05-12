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
  JenkinsConfig jenkinsConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  String jobName;
  Map<String, String> parameters;
  Map<String, String> filePathsForAssertion;
  String activityId;
  String unitName;
  boolean unstableSuccess;
  boolean injectEnvVars;
  JenkinsSubTaskType subTaskType;
  String queuedBuildUrl;
  long timeout;
  long startTs;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    // Ideally we should check for capability for getting encryption details
    // but the original validation task does not do that
    return jenkinsConfig.fetchRequiredExecutionCapabilities();
  }
}
