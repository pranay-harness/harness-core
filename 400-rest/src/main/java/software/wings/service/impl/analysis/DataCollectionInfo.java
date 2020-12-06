package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic Data Collection info for All Verification providers
 * Created by Pranjal on 03/31/2019
 */
@Data
@AllArgsConstructor
public class DataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String cvConfigId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
}
