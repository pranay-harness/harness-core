package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/29/17.
 */
@AllArgsConstructor
public class LogClusterContext {
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
  @Getter private String type;
  @Getter private String stateBaseUrl;
  @Getter private String authToken;

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
    jsonObject.put("type", type);
    jsonObject.put("stateBaseUrl", stateBaseUrl);
    jsonObject.put("authToken", authToken);

    return jsonObject.toString();
  }

  public static LogClusterContext fromJson(String json) {
    JSONObject jsonObject = new JSONObject(json);
    Set<String> controlNodes = jsonObject.isNull("controlNodes")
        ? new HashSet<>()
        : new HashSet<>(Arrays.asList(jsonObject.getString("controlNodes").split(",")));

    return new LogClusterContext(jsonObject.getString("appId"), jsonObject.getString("workflowId"),
        jsonObject.getString("workflowExecutionId"), jsonObject.getString("stateExecutionInstanceId"),
        jsonObject.getString("serviceId"), controlNodes,
        new HashSet<>(Arrays.asList(jsonObject.getString("testNodes").split(","))),
        new HashSet<>(Arrays.asList(jsonObject.getString("query").split(","))), jsonObject.getBoolean("isSSL"),
        jsonObject.getInt("appPort"), jsonObject.getString("accountId"), jsonObject.getString("type"),
        jsonObject.getString("stateBaseUrl"), jsonObject.getString("authToken"));
  }
}
