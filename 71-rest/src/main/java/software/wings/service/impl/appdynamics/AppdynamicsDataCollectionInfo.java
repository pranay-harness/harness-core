package software.wings.service.impl.appdynamics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.AppDynamicsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppdynamicsDataCollectionInfo {
  private AppDynamicsConfig appDynamicsConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private long startTime;
  private long appId;
  private long tierId;
  private int collectionTime;
  private int dataCollectionMinute;
  private Map<String, String> hosts;
  private List<EncryptedDataDetail> encryptedDataDetails;
  @Default private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType = TimeSeriesMlAnalysisType.COMPARATIVE;
}
