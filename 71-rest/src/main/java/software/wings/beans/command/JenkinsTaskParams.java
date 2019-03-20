package software.wings.beans.command;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

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
  JenkinsSubTaskType subTaskType;
  String queuedBuildUrl;
  long timeout;
  long startTs;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return jenkinsConfig.fetchRequiredExecutionCapabilities();
  }
}
