package software.wings.service.impl.splunk;

import java.util.List;

/**
 * Created by rsingh on 6/21/17.
 */
public class SplunkLogRequest {
  private final String applicationId;
  private final String stateExecutionId;
  private final List<String> nodes;
  private final int logCollectionMinute;

  public SplunkLogRequest() {
    applicationId = null;
    stateExecutionId = null;
    nodes = null;
    logCollectionMinute = -1;
  }

  public SplunkLogRequest(String applicationId, String stateExecutionId, int logCollectionMinute, List<String> nodes) {
    this.applicationId = applicationId;
    this.stateExecutionId = stateExecutionId;
    this.logCollectionMinute = logCollectionMinute;
    this.nodes = nodes;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getStateExecutionId() {
    return stateExecutionId;
  }

  public int getLogCollectionMinute() {
    return logCollectionMinute;
  }

  public List<String> getNodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return "SplunkLogRequest{"
        + "applicationId='" + applicationId + '\'' + ", stateExecutionId='" + stateExecutionId + '\''
        + ", nodes=" + nodes + ", logCollectionMinute=" + logCollectionMinute + '}';
  }
}
