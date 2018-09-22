package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private Map<String, String> controlNodes;
  private Map<String, String> testNodes;
  private String query;
  private boolean isSSL;
  private int appPort;
  private AnalysisComparisonStrategy comparisonStrategy;
  private int timeDuration;
  private StateType stateType;
  private String authToken;
  private String analysisServerConfigId;
  private String correlationId;
  private int smooth_window;
  private int tolerance;
  private String prevWorkflowExecutionId;
  private int minimumRequestsPerMinute;
  private int comparisonWindow;
  private int parallelProcesses;
  @Default private Map<String, List<TimeSeries>> timeSeriesToCollect = new HashMap<>();
  private boolean runTillConvergence;

  public LogClusterContext getClusterContext() {
    return LogClusterContext.builder()
        .appId(appId)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .stateExecutionId(stateExecutionId)
        .serviceId(serviceId)
        .controlNodes(controlNodes.keySet())
        .testNodes(testNodes.keySet())
        .query(query)
        .isSSL(isSSL)
        .appPort(appPort)
        .accountId(accountId)
        .stateType(stateType)
        .authToken(authToken)
        .build();
  }
}
