package software.wings.service.intfc.newrelic;

import software.wings.APMFetchConfig;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.encryption.EncryptedDataDetail;
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
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      @NotNull String settingId, long newRelicApplicationId, long instanceId, long fromTime, long toTime);
  RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      NewRelicSetupTestNodeData newRelicSetupTestNodeData);
  Map<String, TimeSeriesMetricDefinition> metricDefinitions(Collection<Metric> metrics);
  Map<String, Metric> getMetricsCorrespondingToMetricNames(List<String> metricNames);
  List<Metric> getListOfMetrics();
}
