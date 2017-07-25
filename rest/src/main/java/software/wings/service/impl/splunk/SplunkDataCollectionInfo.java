package software.wings.service.impl.splunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.SplunkConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplunkDataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String workflowExecutionId;
  private SplunkConfig splunkConfig;
  private Set<String> queries = new HashSet<>();
  private long startTime;
  private int collectionTime;
}
