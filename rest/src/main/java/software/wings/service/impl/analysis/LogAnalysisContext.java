package software.wings.service.impl.analysis;

import com.google.common.collect.Sets;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractLogAnalysisState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@AllArgsConstructor
public class LogAnalysisContext {
  @Getter private String appId;
  @Getter private String workflowId;
  @Getter private String workflowExecutionId;
  @Getter private String stateExecutionInstanceId;
  @Getter private String serviceId;
  @Getter private Set<String> controlNodes;
  @Getter private Set<String> testNodes;
  @Getter private Set<String> queries;
  @Getter private boolean isSSL;
  @Getter private int appPort;
  @Getter private String accountId;
  @Getter private AnalysisComparisonStrategy comparisonStrategy;
  @Getter private String timeDuration;
  @Getter private String type;
  @Getter private String stateBaseUrl;
  @Getter private String authToken;
  @Getter private String analysisServerConfigId;
  @Getter private String correlationId;

  public String toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("appId", appId);
    jsonObject.put("workflowId", workflowId);
    jsonObject.put("workflowExecutionId", workflowExecutionId);
    jsonObject.put("stateExecutionInstanceId", stateExecutionInstanceId);
    jsonObject.put("serviceId", serviceId);
    jsonObject.put("controlNodes", StringUtils.join(controlNodes, ","));
    jsonObject.put("testNodes", StringUtils.join(testNodes, ","));
    jsonObject.put("query", StringUtils.join(queries, ","));
    jsonObject.put("isSSL", isSSL);
    jsonObject.put("appPort", appPort);
    jsonObject.put("accountId", accountId);
    jsonObject.put("comparisonStrategy", comparisonStrategy.name().toString());
    jsonObject.put("timeDuration", timeDuration);
    jsonObject.put("type", type);
    jsonObject.put("stateBaseUrl", stateBaseUrl);
    jsonObject.put("authToken", authToken);
    jsonObject.put("analysisServerConfigId", analysisServerConfigId);
    jsonObject.put("correlationId", correlationId);

    return jsonObject.toString();
  }

  public static LogAnalysisContext fromJson(String json) {
    JSONObject jsonObject = new JSONObject(json);
    Set<String> controlNodes = jsonObject.isNull("controlNodes")
        ? new HashSet<>()
        : new HashSet<>(Arrays.asList(jsonObject.getString("controlNodes").split(",")));

    return new LogAnalysisContext(jsonObject.getString("appId"), jsonObject.getString("workflowId"),
        jsonObject.getString("workflowExecutionId"), jsonObject.getString("stateExecutionInstanceId"),
        jsonObject.getString("serviceId"), controlNodes,
        new HashSet<>(Arrays.asList(jsonObject.getString("testNodes").split(","))),
        new HashSet<>(Arrays.asList(jsonObject.getString("query").split(","))), jsonObject.getBoolean("isSSL"),
        jsonObject.getInt("appPort"), jsonObject.getString("accountId"),
        AnalysisComparisonStrategy.valueOf(jsonObject.getString("comparisonStrategy")),
        jsonObject.getString("timeDuration"), jsonObject.getString("type"), jsonObject.getString("stateBaseUrl"),
        jsonObject.getString("authToken"), jsonObject.getString("analysisServerConfigId"),
        jsonObject.getString("correlationId"));
  }

  public LogClusterContext getClusterContext() {
    return new LogClusterContext(appId, workflowId, workflowExecutionId, stateExecutionInstanceId, serviceId,
        controlNodes, testNodes, queries, isSSL, appPort, accountId, type, stateBaseUrl, authToken);
  }
}
