package software.wings.service.impl.appdynamics;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.AppDynamicsConfig;
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
public class AppdynamicsDataCollectionInfo implements ExecutionCapabilityDemander {
  private AppDynamicsConfig appDynamicsConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private long appId;
  private long tierId;
  private int collectionTime;
  private int dataCollectionMinute;
  private Map<String, String> hosts;
  private boolean nodeIdsMapped;
  private List<EncryptedDataDetail> encryptedDataDetails;
  @Default private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType = TimeSeriesMlAnalysisType.COMPARATIVE;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(appDynamicsConfig, encryptedDataDetails);
  }
}
