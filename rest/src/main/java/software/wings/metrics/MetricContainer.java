package software.wings.metrics;

import software.wings.common.UUIDGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric data for a given build
 * Created by mike@ on 4/14/17.
 */
public class MetricContainer {
  private String uuid;
  // A build is identified by its unique StateExecutionInstance executionUuid.
  private String executionUuid;

  // Pointers to the previous build and its metrics to reduce lookups
  private String previousBuildMetricUuid;
  private String previousBuildExecutionUuid;

  private Map<String, Metric> metricMap;

  public MetricContainer(String executionUuid, String previousBuildMetricUuid, String previousBuildExecutionUuid) {
    this.uuid = UUIDGenerator.getUuid();
    this.executionUuid = executionUuid;
    this.previousBuildMetricUuid = previousBuildMetricUuid;
    this.previousBuildExecutionUuid = previousBuildExecutionUuid;
    this.metricMap = new HashMap<>();
  }

  public MetricContainer(String executionUuid) {
    this.uuid = UUIDGenerator.getUuid();
    this.executionUuid = executionUuid;
    this.previousBuildMetricUuid = null;
    this.previousBuildExecutionUuid = null;
    this.metricMap = new HashMap<>();
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getExecutionUuid() {
    return executionUuid;
  }

  public void setExecutionUuid(String executionUuid) {
    this.executionUuid = executionUuid;
  }

  public String getPreviousBuildMetricUuid() {
    return previousBuildMetricUuid;
  }

  public void setPreviousBuildMetricUuid(String previousBuildMetricUuid) {
    this.previousBuildMetricUuid = previousBuildMetricUuid;
  }

  public String getPreviousBuildExecutionUuid() {
    return previousBuildExecutionUuid;
  }

  public void setPreviousBuildExecutionUuid(String previousBuildExecutionUuid) {
    this.previousBuildExecutionUuid = previousBuildExecutionUuid;
  }

  public Map<String, Metric> getMetricMap() {
    return metricMap;
  }

  public void setMetricMap(Map<String, Metric> metricMap) {
    this.metricMap = metricMap;
  }

  public void addMetric(Metric metric) {
    this.metricMap.put(metric.getUuid(), metric);
  }

  public void removeMetric(Metric metric) {
    this.metricMap.remove(metric.getUuid());
  }
}
