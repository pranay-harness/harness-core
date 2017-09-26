package software.wings.service.impl.appdynamics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.AppDynamicsConfig;

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
}
