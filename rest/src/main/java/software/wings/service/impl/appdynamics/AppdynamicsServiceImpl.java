package software.wings.service.impl.appdynamics;

import static software.wings.beans.DelegateTask.Context.Builder.aContext;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ArrayListMultimap;

import software.wings.api.AppDynamicsExecutionData;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.Context;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.BucketData;
import software.wings.metrics.MetricCalculator;
import software.wings.metrics.MetricDefinition;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AppdynamicsServiceImpl implements AppdynamicsService {
  private static final long APPDYNAMICS_CALL_TIMEOUT = TimeUnit.MINUTES.toMillis(1L);

  @com.google.inject.Inject private SettingsService settingsService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public List<AppdynamicsApplication> getApplications(final String settingId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getAllApplications((AppDynamicsConfig) settingAttribute.getValue());
  }

  @Override
  public List<AppdynamicsTier> getTiers(String settingId, int appdynamicsAppId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getTiers((AppDynamicsConfig) settingAttribute.getValue(), appdynamicsAppId);
  }

  @Override
  public List<AppdynamicsNode> getNodes(String settingId, int appdynamicsAppId, int tierId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getNodes((AppDynamicsConfig) settingAttribute.getValue(), appdynamicsAppId, tierId);
  }

  @Override
  public List<AppdynamicsBusinessTransaction> getBusinessTransactions(String settingId, long appdynamicsAppId)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();

    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getBusinessTransactions((AppDynamicsConfig) settingAttribute.getValue(), appdynamicsAppId);
  }

  @Override
  public List<AppdynamicsMetric> getTierBTMetrics(String settingId, long appdynamicsAppId, long tierId)
      throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    context.setTimeOut(APPDYNAMICS_CALL_TIMEOUT);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getTierBTMetrics((AppDynamicsConfig) settingAttribute.getValue(), appdynamicsAppId, tierId);
  }

  @Override
  public List<AppdynamicsMetricData> getTierBTMetricData(
      String settingId, int appdynamicsAppId, int tierId, String btName, int durantionInMinutes) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(settingId);
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    context.setTimeOut(APPDYNAMICS_CALL_TIMEOUT);
    return delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
        .getTierBTMetricData(
            (AppDynamicsConfig) settingAttribute.getValue(), appdynamicsAppId, tierId, btName, durantionInMinutes);
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute) {
    try {
      Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      delegateProxyFactory.get(AppdynamicsDelegateService.class, context)
          .validateConfig((AppDynamicsConfig) settingAttribute.getValue());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public Boolean saveMetricData(
      String accountId, long appdynamicsAppId, long tierId, List<AppdynamicsMetricData> metricDataList) {
    for (AppdynamicsMetricData metricData : metricDataList) {
      List<AppdynamicsMetricDataRecord> metricDataRecords =
          AppdynamicsMetricDataRecord.generateDataRecords(accountId, appdynamicsAppId, tierId, metricData);
      wingsPersistence.saveIgnoringDuplicateKeys(metricDataRecords);
    }
    return true;
  }

  public Map<String, Map<String, BucketData>> generateMetrics(
      String stateExecutionInstanceId, String accountId, String appId) {
    PageRequest.Builder requestBuilder =
        aPageRequest()
            .addFilter("stateExecutionInstanceId", Operator.EQ, stateExecutionInstanceId)
            .addFilter("accountId", Operator.EQ, accountId);
    PageResponse<BucketData> response = wingsPersistence.query(BucketData.class, requestBuilder.build());
    // If there are matching BucketData records, it means this state has completed and return the summary written to the
    // persistence layer.
    if (response.getResponse() != null && response.getResponse().size() > 0) {
      Map<String, Map<String, BucketData>> metricSummaries = new HashMap<>();
      for (BucketData bucketData : response.getResponse()) {
        if (metricSummaries.get(bucketData.getBtId()) == null) {
          metricSummaries.put(bucketData.getBtId(), new HashMap<>());
        }
        metricSummaries.get(bucketData.getBtId()).put(bucketData.getMetricName(), bucketData);
      }
      return metricSummaries;
    }

    // Otherwise, generate the metrics from the AppdynamicsMetricDataRecords.
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
    AppDynamicsExecutionData appDynamicsExecutionData =
        (AppDynamicsExecutionData) stateExecutionInstance.getStateExecutionMap().get(StateType.APP_DYNAMICS.getName());
    long appdynamicsAppId = appDynamicsExecutionData.getAppDynamicsApplicationId();
    long tierId = appDynamicsExecutionData.getAppdynamicsTierId();
    List<String> btList = appDynamicsExecutionData.getBtNames();
    List<String> newNodeNames = appDynamicsExecutionData.getCanaryNewHostNames();
    long startTime = appDynamicsExecutionData.getStartTs();
    PageRequest.Builder amdrRequestBuilder = aPageRequest()
                                                 .addFilter("accountId", Operator.EQ, accountId)
                                                 .addFilter("appdAppId", Operator.EQ, appdynamicsAppId)
                                                 .addFilter("tierId", Operator.EQ, tierId)
                                                 .addFilter("btName", Operator.IN, btList.toArray())
                                                 //        .addFilter("startTime", Operator.GT, startTime - 1)
                                                 //        .addFilter("startTime", Operator.LT, endTimeInMillis)
                                                 .addOrder("startTime", OrderType.ASC);
    PageResponse<AppdynamicsMetricDataRecord> amdrResponse =
        wingsPersistence.query(AppdynamicsMetricDataRecord.class, amdrRequestBuilder.build());
    ArrayListMultimap<String, AppdynamicsMetricDataRecord> dataMap = ArrayListMultimap.create();
    Set<Long> metricIds = new HashSet<>();
    for (AppdynamicsMetricDataRecord record : amdrResponse.getResponse()) {
      dataMap.put(record.getBtName(), record);
      metricIds.add(record.getMetricId());
    }
    requestBuilder = aPageRequest()
                         .addFilter("accountId", Operator.EQ, accountId)
                         //        .addFilter("appdynamicsAppId", Operator.EQ, appdynamicsAppId)
                         .addFilter("metricId", Operator.IN, metricIds);
    PageResponse<MetricDefinition> metricDefinitions =
        wingsPersistence.query(MetricDefinition.class, requestBuilder.build());
    Map<String, Map<String, BucketData>> metricSummaries =
        MetricCalculator.calculateMetrics(metricDefinitions.getResponse(), dataMap, newNodeNames);
    return metricSummaries;
  }
}
