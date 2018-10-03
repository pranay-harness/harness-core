package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.StateType;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/29/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogClusterContext {
  private String accountId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;
  private String serviceId;
  private Set<String> controlNodes;
  private Set<String> testNodes;
  private String query;
  private boolean isSSL;
  private int appPort;
  private StateType stateType;
  private String stateBaseUrl;
}
