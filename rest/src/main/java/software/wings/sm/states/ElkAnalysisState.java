package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.LogCollectionCallback;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkSettingProvider;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by raghu on 8/4/17.
 */
public class ElkAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient protected final static String DEFAULT_TIME_FIELD = "@timestamp";

  @SchemaIgnore @Transient protected final static String DEFAULT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(ElkAnalysisState.class);

  @Transient @Inject protected ElkAnalysisService elkAnalysisService;

  @Attributes(required = true, title = "Elastic Search Server") protected String analysisServerConfigId;

  @Attributes(title = "Elastic search indices to search", required = true)
  @DefaultValue("_all")
  protected String indices;

  @Attributes(required = true, title = "Hostname Field") @DefaultValue("beat.hostname") protected String hostnameField;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  public ElkAnalysisState(String name) {
    super(name, StateType.ELK.getType());
  }

  public ElkAnalysisState(String name, String type) {
    super(name, type);
  }

  public String getIndices() {
    return indices;
  }

  public void setIndices(String indices) {
    this.indices = indices;
  }

  public String getHostnameField() {
    return hostnameField;
  }

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  public String getMessageField() {
    return messageField;
  }

  public void setMessageField(String messageField) {
    this.messageField = messageField;
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

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue(".*exception.*")
  public String getQuery() {
    return query;
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    final String timestampField = DEFAULT_TIME_FIELD;
    final String accountId = appService.get(context.getAppId()).getAccountId();
    final String timestampFieldFormat = getTimestmpFieldFormat(accountId, timestampField);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final ElkDataCollectionInfo dataCollectionInfo =
        new ElkDataCollectionInfo(elkConfig, appService.get(context.getAppId()).getAccountId(), context.getAppId(),
            context.getStateExecutionInstanceId(), getWorkflowId(context), context.getWorkflowExecutionId(),
            getPhaseServiceId(context), queries, indices, hostnameField, messageField, timestampField,
            timestampFieldFormat, logCollectionStartTimeStamp, Integer.parseInt(timeDuration), hosts);
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.ELK_COLLECT_LOG_DATA)
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
  @EnumData(enumDataProvider = ElkSettingProvider.class)
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
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
    analysisService.finalizeLogCollection(accountId, StateType.ELK, context.getWorkflowExecutionId(), logRequest);
  }

  private String getTimestmpFieldFormat(String accountId, String timestampField) {
    try {
      Map<String, ElkIndexTemplate> indexTemplateMap = elkAnalysisService.getIndices(accountId, analysisServerConfigId);
      final ElkIndexTemplate indexTemplate = indexTemplateMap.get(indices);
      Preconditions.checkNotNull(indexTemplate, "No index template mapping found for " + indices);

      final Object timeStampObject = indexTemplate.getProperties().get(timestampField);
      Preconditions.checkNotNull(
          timeStampObject, timestampField + " is not configured in the index mapping " + indices);
      JSONObject timeStampJsonObject = new JSONObject(JsonUtils.asJson(timeStampObject));

      if (!timeStampJsonObject.has("format")) {
        return DEFAULT_TIME_FORMAT;
      }
      return timeStampJsonObject.getString("format");
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
