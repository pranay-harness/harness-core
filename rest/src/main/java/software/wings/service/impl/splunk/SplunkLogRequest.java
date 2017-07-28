package software.wings.service.impl.splunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.utils.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SplunkLogRequest {
  private String query;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private Set<String> nodes;
  private int logCollectionMinute;
}
