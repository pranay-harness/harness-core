package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.time.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.DelegateTask;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DynatraceState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(DynatraceState.class);
  @Transient @SchemaIgnore public static final String TEST_HOST_NAME = "testNode";
  @Transient @SchemaIgnore public static final String CONTROL_HOST_NAME = "controlNode";

  @Attributes(required = true, title = "Dynatrace Server") private String analysisServerConfigId;

  @Attributes(required = true, title = "Service Methods") private String serviceMethods;

  public DynatraceState(String name) {
    super(name, StateType.DYNA_TRACE);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      MetricAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    Preconditions.checkNotNull(settingAttribute, "No dynatrace setting with id: " + analysisServerConfigId + " found");

    final DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingAttribute.getValue();

    final long dataCollectionStartTimeStamp = Timestamp.currentMinuteBoundary();
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
            .serviceMethods(splitServiceMethods(serviceMethods))
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                dynaTraceConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .analysisComparisonStrategy(getComparisonStrategy())
            .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .accountId(appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .waitId(waitId)
            .data(TaskData.builder()
                      .taskType(TaskType.DYNA_TRACE_METRIC_DATA_COLLECTION_TASK.name())
                      .parameters(new Object[] {dataCollectionInfo})
                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 5))
                      .build())
            .envId(envId)
            .infrastructureMappingId(infrastructureMappingId)
            .build();
    waitNotifyEngine.waitForAll(DataCollectionCallback.builder()
                                    .appId(context.getAppId())
                                    .executionData(executionData)
                                    .isLogCollection(false)
                                    .build(),
        waitId);
    return delegateService.queueTask(delegateTask);
  }

  public static Set<String> splitServiceMethods(String serviceMethods) {
    Set<String> methodNames = new HashSet<>();
    String[] methods = serviceMethods.split("\n");
    for (String method : methods) {
      methodNames.add(method.trim());
    }
    return methodNames;
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
}
