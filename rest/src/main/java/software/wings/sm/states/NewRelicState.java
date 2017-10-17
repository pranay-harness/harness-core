package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;

import java.util.Set;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(NewRelicState.class);

  @EnumData(enumDataProvider = NewRelicSettingProvider.class)
  @Attributes(required = true, title = "New Relic Server")
  private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  public NewRelicState(String name) {
    super(name, StateType.NEW_RELIC);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (StringUtils.isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No new relic setting with id: " + analysisServerConfigId + " found");
    }

    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    final long dataCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final NewRelicDataCollectionInfo dataCollectionInfo = NewRelicDataCollectionInfo.builder()
                                                              .newRelicConfig(newRelicConfig)
                                                              .applicationId(context.getAppId())
                                                              .stateExecutionId(context.getStateExecutionInstanceId())
                                                              .workflowId(getWorkflowId(context))
                                                              .workflowExecutionId(context.getWorkflowExecutionId())
                                                              .serviceId(getPhaseServiceId(context))
                                                              .startTime(dataCollectionStartTimeStamp)
                                                              .collectionTime(Integer.parseInt(timeDuration))
                                                              .newRelicAppId(Long.parseLong(applicationId))
                                                              .dataCollectionMinute(0)
                                                              .build();

    String waitId = UUIDGenerator.getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId), waitId);
    return delegateService.queueTask(delegateTask);
  }

  @SchemaIgnore
  protected String getStateBaseUrl() {
    return "newrelic";
  }
}
