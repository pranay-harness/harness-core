package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
  private String query;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String serviceId;
  private Set<String> nodes;
  private long logCollectionMinute;
  private boolean isExperimental;
}
