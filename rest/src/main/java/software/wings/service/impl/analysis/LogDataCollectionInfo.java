package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 8/8/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private Set<String> queries = new HashSet<>();
  private long startTime;
  private int collectionTime;
  private Set<String> hosts;
  private StateType stateType;
}
