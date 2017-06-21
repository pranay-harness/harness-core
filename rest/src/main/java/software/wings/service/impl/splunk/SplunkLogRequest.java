package software.wings.service.impl.splunk;

import software.wings.utils.JsonUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsingh on 6/21/17.
 */
public class SplunkLogRequest {
  private final String applicationId;
  private final long startTime;
  private final long endTime;
  private final List<String> nodes;

  public SplunkLogRequest() {
    applicationId = null;
    startTime = -1;
    endTime = -1;
    nodes = null;
  }

  public SplunkLogRequest(String applicationId, long startTime, long endTime, List<String> nodes) {
    this.applicationId = applicationId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.nodes = nodes;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public List<String> getNodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return "SplunkLogRequest{"
        + "applicationId='" + applicationId + '\'' + ", startTime=" + startTime + ", endTime=" + endTime
        + ", nodes=" + nodes + '}';
  }

  public static void main(String[] args) {
    SplunkLogRequest logRequest = new SplunkLogRequest(
        "m9XTWIcnS2OVk-ys0wiX-Q", 1497945240000L, 1497945241000L, Collections.singletonList("ip-172-31-11-228"));
    System.out.println(JsonUtils.asJson(logRequest));
  }
}
