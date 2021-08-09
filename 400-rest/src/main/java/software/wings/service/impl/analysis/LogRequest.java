package software.wings.service.impl.analysis;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
