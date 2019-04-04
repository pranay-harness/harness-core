package software.wings.service.impl.newrelic;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.NewRelicConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewRelicDataCollectionInfo implements ExecutionCapabilityDemander {
  private NewRelicConfig newRelicConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private long startTime;
  private int collectionTime;
  private long newRelicAppId;
  private int dataCollectionMinute;
  List<EncryptedDataDetail> encryptedDataDetails;
  private Map<String, String> hosts;
  private String settingAttributeId;
  private String deploymentMarker;
  private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType;
  private String cvConfigId;
  private boolean checkNotAllowedStrings;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(newRelicConfig, encryptedDataDetails);
  }
}
