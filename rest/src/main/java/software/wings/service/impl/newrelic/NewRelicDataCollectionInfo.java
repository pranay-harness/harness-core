package software.wings.service.impl.newrelic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.NewRelicConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewRelicDataCollectionInfo {
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
}
