/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.newrelic;

import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.APMFetchConfig;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.SettingAttribute;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicSetupTestNodeData;
import software.wings.sm.StateType;
import software.wings.sm.states.NewRelicState.Metric;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicService {
  void validateConfig(@NotNull SettingAttribute settingAttribute, @NotNull StateType stateType,
      List<EncryptedDataDetail> encryptedDataDetails);
  void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config);
  List<NewRelicApplication> getApplications(@NotNull String settingId, @NotNull StateType stateType);
  String fetch(String accountId, String serverConfigId, APMFetchConfig url);
  List<NewRelicApplicationInstance> getApplicationInstances(
      @NotNull String settingId, long applicationId, @NotNull StateType stateType);
  List<NewRelicMetric> getTxnsWithData(String settingId, long applicationId, long instanceId);
  RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      NewRelicSetupTestNodeData newRelicSetupTestNodeData);
  Map<String, TimeSeriesMetricDefinition> metricDefinitions(Collection<Metric> metrics);
  Map<String, Metric> getMetricsCorrespondingToMetricNames(List<String> metricNames);
  List<Metric> getListOfMetrics();
  NewRelicApplication resolveApplicationName(String settingId, String newRelicApplicationName);
  NewRelicApplication resolveApplicationName(
      String settingId, String newRelicApplicationName, String appId, String workflowExecutionId);
  NewRelicApplication resolveApplicationId(String settingId, String newRelicApplicationId);
  NewRelicApplication resolveApplicationId(
      String settingId, String newRelicApplicationId, String appId, String workflowExecutionId);
}
