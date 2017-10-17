package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.StateType;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisContext {
  private String accountId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;
  private String serviceId;
  private Set<String> controlNodes;
  private Set<String> testNodes;
  private Set<String> queries;
  private boolean isSSL;
  private int appPort;
  private AnalysisComparisonStrategy comparisonStrategy;
  private int timeDuration;
  private StateType stateType;
  private String stateBaseUrl;
  private String authToken;
  private String analysisServerConfigId;
  private String correlationId;
  private int smooth_window;
  private int tolerance;

  public LogClusterContext getClusterContext() {
    return LogClusterContext.builder()
        .appId(appId)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .serviceId(serviceId)
        .controlNodes(controlNodes)
        .testNodes(testNodes)
        .queries(queries)
        .isSSL(isSSL)
        .appPort(appPort)
        .accountId(accountId)
        .stateType(stateType)
        .stateBaseUrl(stateBaseUrl)
        .authToken(authToken)
        .build();
  }
}
