package software.wings.service.impl.splunk;

import lombok.Data;
import software.wings.beans.SplunkConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
public class SplunkDataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private SplunkConfig splunkConfig;
  private Set<String> queries = new HashSet<>();
  private long startTime;
  private int collectionTime;

  public SplunkDataCollectionInfo() {}

  public SplunkDataCollectionInfo(String accountId, String applicationId, String stateExecutionId,
      SplunkConfig splunkConfig, Set<String> queries, long startTime, int collectionTime) {
    this.accountId = accountId;
    this.applicationId = applicationId;
    this.stateExecutionId = stateExecutionId;
    this.splunkConfig = splunkConfig;
    this.queries = queries;
    this.startTime = startTime;
    this.collectionTime = collectionTime;
  }
}
