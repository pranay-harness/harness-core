package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.Sets;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.config.LogzConfig;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.logz.LogzSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/21/17.
 */
public class LogzAnalysisState extends ElkAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(LogzAnalysisState.class);

  public LogzAnalysisState(String name) {
    super(name, StateType.LOGZ.getType());
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No logz setting with id: " + analysisServerConfigId + " found");
    }

    final LogzConfig logzConfig = (LogzConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final LogzDataCollectionInfo dataCollectionInfo =
        new LogzDataCollectionInfo(logzConfig, appService.get(context.getAppId()).getAccountId(), context.getAppId(),
            context.getStateExecutionInstanceId(), getWorkflowId(context), context.getWorkflowExecutionId(),
            getPhaseServiceId(context), queries, hostnameField, messageField, DEFAULT_TIME_FIELD, DEFAULT_TIME_FORMAT,
            logCollectionStartTimeStamp, 0, Integer.parseInt(timeDuration), hosts,
            kmsService.getEncryptionDetails(logzConfig, context.getWorkflowId(), context.getAppId()));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.LOGZ_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 5))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId), waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  @EnumData(enumDataProvider = LogzSettingProvider.class)
  @Attributes(required = true, title = "Logz Server")
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
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

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Failure Criteria")
  @DefaultValue("LOW")
  public AnalysisTolerance getAnalysisTolerance() {
    if (StringUtils.isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (StringUtils.isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue(".*[e|E]xception.*")
  public String getQuery() {
    return query;
  }

  @SchemaIgnore
  public String getIndices() {
    return indices;
  }

  @Attributes(required = true, title = "Hostname Field")
  @DefaultValue("hostname")
  public String getHostnameField() {
    return hostnameField;
  }

  @Attributes(required = true, title = "Message Field")
  @DefaultValue("message")
  public String getMessageField() {
    return messageField;
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }
}
