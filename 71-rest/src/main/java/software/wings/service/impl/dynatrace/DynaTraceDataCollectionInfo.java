package software.wings.service.impl.dynatrace;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.DynaTraceConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynaTraceDataCollectionInfo implements ExecutionCapabilityDemander {
  private DynaTraceConfig dynaTraceConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private int collectionTime;
  private List<DynaTraceTimeSeries> timeSeriesDefinitions;
  private Set<String> serviceMethods;
  private AnalysisComparisonStrategy analysisComparisonStrategy;
  private int dataCollectionMinute;
  List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(dynaTraceConfig, encryptedDataDetails);
  }
}
