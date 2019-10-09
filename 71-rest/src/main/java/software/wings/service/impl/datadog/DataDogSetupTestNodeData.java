package software.wings.service.impl.datadog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState.Metric;

import java.util.Map;
import java.util.Set;

/**
 * Created by Pranjal on 10/23/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DataDogSetupTestNodeData extends SetupTestNodeData {
  private String datadogServiceName;
  private Map<String, String> dockerMetrics;
  private Map<String, String> ecsMetrics;
  private Map<String, Set<Metric>> customMetricsMap;
  private String metrics;
  private String hostNameField;
  private String query;
  private Map<String, Set<Metric>> customMetrics;
  private String deploymentType;

  @Builder
  public DataDogSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      InstanceElement instanceElement, String hostExpression, String workflowId, String guid, long fromTime,
      long toTime, String datadogServiceName, Map<String, String> dockerMetrics, Map<String, String> ecsMetrics,
      Map<String, Set<Metric>> customMetricsMap, StateType stateType, String metrics, String query,
      String hostNameField, Map<String, Set<Metric>> customMetrics, String deploymentType) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid, stateType,
        fromTime, toTime);
    this.datadogServiceName = datadogServiceName;
    this.dockerMetrics = dockerMetrics;
    this.ecsMetrics = ecsMetrics;
    this.customMetricsMap = customMetricsMap;
    this.metrics = metrics;
    this.customMetrics = customMetrics;
    this.deploymentType = deploymentType;
    this.query = query;
    this.hostNameField = hostNameField;
  }
}
