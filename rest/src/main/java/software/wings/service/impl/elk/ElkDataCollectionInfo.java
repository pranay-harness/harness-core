package software.wings.service.impl.elk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ElkConfig;
import software.wings.beans.SplunkConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElkDataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private ElkConfig elkConfig;
  private Set<String> queries = new HashSet<>();
  private Set<String> hosts;
  private long startTime;
  private int collectionTime;
}
