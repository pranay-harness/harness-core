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
import software.wings.service.impl.analysis.LogCollectionCallback;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.logz.LogzSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 8/21/17.
 */
public class LogzAnalysisState extends ElkAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(LogzAnalysisState.class);

  public LogzAnalysisState(String name) {
    super(name, StateType.LOGZ.getType());
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    final LogzConfig logzConfig = (LogzConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final LogzDataCollectionInfo dataCollectionInfo =
        new LogzDataCollectionInfo(logzConfig, appService.get(context.getAppId()).getAccountId(), context.getAppId(),
            context.getStateExecutionInstanceId(), getWorkflowId(context), context.getWorkflowExecutionId(),
            getPhaseServiceId(context), queries, hostnameField, messageField, timestampField, timestampFieldFormat,
            logCollectionStartTimeStamp, Integer.parseInt(timeDuration), hosts);
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.LOGZ_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .build();
    waitNotifyEngine.waitForAll(new LogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
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

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (StringUtils.isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Attributes(required = true, title = "Search Keywords", description = "Such as *Exception*")
  @DefaultValue(".*exception.*")
  public String getQuery() {
    return query;
  }

  @SchemaIgnore
  public String getIndices() {
    return indices;
  }

  @Attributes(required = true, title = "Hostname Field", description = "Hostname field mapping in elastic search")
  @DefaultValue("beat.hostname")
  public String getHostnameField() {
    return hostnameField;
  }

  @Attributes(required = true, title = "Message Field", description = "Message field mapping in elastic search")
  @DefaultValue("message")
  public String getMessageField() {
    return messageField;
  }

  @Attributes(required = true, title = "Timestamp Field", description = "Timestamp field mapping in elastic search")
  @DefaultValue("@timestamp")
  public String getTimestampField() {
    return timestampField;
  }

  @Attributes(
      required = true, title = "Timestamp Field Format", description = "Timestamp field format in elastic search")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  public String
  getTimestampFieldFormat() {
    return timestampFieldFormat;
  }

  @Override
  protected void preProcess(ExecutionContext context, int logAnalysisMinute) {
    Set<String> testNodes = getCanaryNewHostNames(context);
    Set<String> controlNodes = getLastExecutionNodes(context);
    Set<String> allNodes = new HashSet<>();

    if (controlNodes != null) {
      allNodes.addAll(controlNodes);
    }

    if (testNodes != null) {
      allNodes.addAll(testNodes);
    }

    final String accountId = appService.get(context.getAppId()).getAccountId();
    String serviceId = getPhaseServiceId(context);
    LogRequest logRequest = new LogRequest(query, context.getAppId(), context.getStateExecutionInstanceId(),
        getWorkflowId(context), serviceId, allNodes, logAnalysisMinute);
    analysisService.finalizeLogCollection(accountId, StateType.LOGZ, context.getWorkflowExecutionId(), logRequest);
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }
}
