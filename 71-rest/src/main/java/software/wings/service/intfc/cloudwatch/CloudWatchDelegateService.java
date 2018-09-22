package software.wings.service.intfc.cloudwatch;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 4/2/18.
 */
public interface CloudWatchDelegateService {
  @DelegateTaskType(TaskType.CLOUD_WATCH_METRIC_DATA_FOR_NODE)
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(AwsConfig config,
      List<EncryptedDataDetail> encryptionDetails, CloudWatchSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog thirdPartyApiCallLog, String hostName) throws IOException;
}
