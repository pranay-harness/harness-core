package software.wings.service.intfc.stackdriver;

import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.stackdriver.StackDriverNameSpace;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by Pranjal on 11/27/2018
 */
public interface StackDriverDelegateService {
  @DelegateTaskType(TaskType.STACKDRIVER_METRIC_DATA_FOR_NODE)
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(@NotNull GcpConfig appDynamicsConfig,
      List<EncryptedDataDetail> encryptionDetails, StackDriverSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) throws IOException, CloneNotSupportedException;

  @DelegateTaskType(TaskType.STACKDRIVER_LIST_REGIONS)
  List<String> listRegions(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException;

  @DelegateTaskType(TaskType.STACKDRIVER_LIST_FORWARDING_RULES)
  Map<String, String> listForwardingRules(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String region) throws IOException;

  String createFilter(StackDriverNameSpace nameSpace, String metric, String hostName, String ruleName);

  String getProjectId(GcpConfig gcpConfig);

  long getTimeStamp(String data);

  String getDateFormatTime(long time);
}
