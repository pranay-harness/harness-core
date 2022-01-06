/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.datadog;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState.Metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
      Instance instanceElement, String hostExpression, String workflowId, String guid, long fromTime, long toTime,
      String datadogServiceName, Map<String, String> dockerMetrics, Map<String, String> ecsMetrics,
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
