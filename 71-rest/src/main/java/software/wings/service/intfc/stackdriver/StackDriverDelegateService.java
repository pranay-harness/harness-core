package software.wings.service.intfc.stackdriver;

import com.google.api.services.logging.v2.model.LogEntry;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.stackdriver.StackDriverNameSpace;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  String createFilter(StackDriverNameSpace nameSpace, String metric, String dimensionValue);

  String getProjectId(GcpConfig gcpConfig);

  long getTimeStamp(String data);

  String getDateFormatTime(long time);

  List<LogEntry> fetchLogs(String query, long startTime, long endTime, ThirdPartyApiCallLog apiCallLog,
      Set<String> hosts, String hostnameField, GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      boolean is24X7Task);

  @DelegateTaskType(TaskType.STACKDRIVER_LOG_DATA_FOR_NODE)
  VerificationNodeDataSetupResponse getLogWithDataForNode(GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails, String query, long startTime, long endTime,
      ThirdPartyApiCallLog apiCallLog, Set<String> hosts, String hostnameField, long logCollectionMinute,
      boolean is24X7Task, String logMessageField);
}
