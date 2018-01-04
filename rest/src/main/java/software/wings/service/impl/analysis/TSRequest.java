package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 9/19/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TSRequest {
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private Set<String> nodes;
  private int analysisMinute;
  private int analysisStartMinute;
}
