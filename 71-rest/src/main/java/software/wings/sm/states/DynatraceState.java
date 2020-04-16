package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
@FieldNameConstants(innerTypeName = "DynatraceStateKeys")
public class DynatraceState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore public static final String TEST_HOST_NAME = "testNode";
  @Transient @SchemaIgnore public static final String CONTROL_HOST_NAME = "controlNode";
  @Inject @SchemaIgnore private transient DynaTraceService dynaTraceService;
  @Attributes(required = true, title = "Dynatrace Server") private String analysisServerConfigId;

  private String serviceEntityId;

  public DynatraceState(String name) {
    super(name, StateType.DYNA_TRACE);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    final String resolvedConnectorId =
        getResolvedConnectorId(context, DynatraceStateKeys.analysisServerConfigId, analysisServerConfigId);
    String envId = getEnvId(context);

    final SettingAttribute settingAttribute = settingsService.get(resolvedConnectorId);
    Preconditions.checkNotNull(settingAttribute, "No dynatrace setting with id: " + analysisServerConfigId + " found");
    final long dataCollectionStartTimeStamp = dataCollectionStartTimestampMillis();
    final DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingAttribute.getValue();

    String resolvedServiceField = getResolvedFieldValue(context, DynatraceStateKeys.serviceEntityId, serviceEntityId);
    String resolvedServiceId = null;

    // we will do the resolution for serviceID only if it is not empty
    // this is because already existing setups will not have this field.
    if (isNotEmpty(resolvedServiceField)) {
      // if this is a name, get the corresponding ID.
      try {
        resolvedServiceId = dynaTraceService.resolveDynatraceServiceNameToId(resolvedConnectorId, resolvedServiceField);
      } catch (Exception ex) {
        logger.info("Exception while trying to resolve dynatrace service name to id");
      }
      if (resolvedServiceId == null) {
        boolean isValidId = dynaTraceService.validateDynatraceServiceId(resolvedConnectorId, resolvedServiceField);
        if (!isValidId) {
          throw new DataCollectionException("Invalid serviceId provided in setup: " + resolvedServiceField);
        }
        resolvedServiceId = resolvedServiceField;
      }
    }

    final DynaTraceDataCollectionInfo dataCollectionInfo =
        DynaTraceDataCollectionInfo.builder()
            .dynaTraceConfig(dynaTraceConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(context.getWorkflowId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .timeSeriesDefinitions(Lists.newArrayList(DynaTraceTimeSeries.values()))
            .dataCollectionMinute(0)
            .dynatraceServiceId(resolvedServiceId)
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                dynaTraceConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .analysisComparisonStrategy(getComparisonStrategy())
            .build();

    String waitId = generateUuid();
    String infrastructureMappingId = context.fetchInfraMappingId();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .waitId(waitId)
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.DYNA_TRACE_METRIC_DATA_COLLECTION_TASK.name())
                      .parameters(new Object[] {dataCollectionInfo})
                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 5))
                      .build())
            .envId(envId)
            .infrastructureMappingId(infrastructureMappingId)
            .build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION,
        DataCollectionCallback.builder()
            .appId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .dataCollectionStartTime(dataCollectionStartTimeStamp)
            .dataCollectionEndTime(
                dataCollectionStartTimeStamp + TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration())))
            .executionData(executionData)
            .build(),
        waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  protected Map<String, String> getLastExecutionNodes(ExecutionContext context) {
    Map<String, String> controlHostMap = new HashMap<>();
    for (int i = 1; i <= CANARY_DAYS_TO_COLLECT; i++) {
      controlHostMap.put(CONTROL_HOST_NAME + i, DEFAULT_GROUP_NAME);
    }
    return controlHostMap;
  }

  @Override
  protected Map<String, String> getCanaryNewHostNames(ExecutionContext context) {
    return Collections.singletonMap(TEST_HOST_NAME, DEFAULT_GROUP_NAME);
  }

  public static String getMetricTypeForMetric(String metricName) {
    List<DynaTraceTimeSeries> timeSeriesList = Lists.newArrayList(DynaTraceTimeSeries.values());
    return timeSeriesList.stream()
        .filter(timeSeries -> timeSeries.getSavedFieldName().equals(metricName))
        .findAny()
        .map(timeSeries -> timeSeries.getMetricType().name())
        .orElse(null);
  }
}
