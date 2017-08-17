package software.wings.service.impl.analysis;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 8/8/17.
 */
@Data
@NoArgsConstructor
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

  public LogDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, Set<String> queries, long startTime, int collectionTime,
      Set<String> hosts) {
    this.accountId = accountId;
    this.applicationId = applicationId;
    this.stateExecutionId = stateExecutionId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.serviceId = serviceId;
    this.queries = queries;
    this.startTime = startTime;
    this.collectionTime = collectionTime;
    this.hosts = hosts;
  }
}
