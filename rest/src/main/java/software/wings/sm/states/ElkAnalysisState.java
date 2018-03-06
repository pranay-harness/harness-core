package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkLogFetchRequest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by raghu on 8/4/17.
 */
public class ElkAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient protected static final String DEFAULT_TIME_FIELD = "@timestamp";

  @SchemaIgnore @Transient protected static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(ElkAnalysisState.class);

  @Transient @Inject protected ElkAnalysisService elkAnalysisService;

  @Attributes(required = true, title = "Elastic Search Server") protected String analysisServerConfigId;

  @Attributes(title = "Elastic search indices to search", required = true)
  @DefaultValue("_all")
  protected String indices;

  @Attributes(required = true, title = "Hostname or Container Id Field")
  @DefaultValue("beat.hostname")
  protected String hostnameField;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  @Attributes(required = true, title = "Timestamp format")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private String timestampFormat;

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
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Failure Criteria")
  @DefaultValue("LOW")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("error")
  public String getQuery() {
    return query;
  }

  @Attributes(title = "Execute with previous steps")
  public boolean getExecuteWithPreviousSteps() {
    return super.isExecuteWithPreviousSteps();
  }

  public String getTimestampFormat() {
    if (timestampFormat == null) {
      return DEFAULT_TIME_FORMAT;
    }
    return timestampFormat;
  }

  public void setTimestampFormat(String format) {
    this.timestampFormat = format;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    final String timestampField = DEFAULT_TIME_FIELD;
    final String accountId = appService.get(context.getAppId()).getAccountId();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = null;
    String finalAnalysisServerConfigId = analysisServerConfigId;
    String finalIndices = context.renderExpression(indices);

    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        finalAnalysisServerConfigId = settingAttribute.getUuid();
      }
      TemplateExpression indicesExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "indices");
      if (indicesExpression != null) {
        finalIndices = templateExpressionProcessor.resolveTemplateExpression(context, indicesExpression);
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(finalAnalysisServerConfigId);
    }
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + finalAnalysisServerConfigId + " found");
    }
    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();

    final String timestampFieldFormat =
        getTimestampFieldFormat(accountId, finalAnalysisServerConfigId, finalIndices, timestampField);
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());

    List<Set<String>> batchedHosts = batchHosts(hosts);
    String[] waitIds = new String[batchedHosts.size()];
    List<DelegateTask> delegateTasks = new ArrayList<>();
    int i = 0;
    for (Set<String> hostBatch : batchedHosts) {
      final ElkDataCollectionInfo dataCollectionInfo = new ElkDataCollectionInfo(elkConfig, accountId,
          context.getAppId(), context.getStateExecutionInstanceId(), getWorkflowId(context),
          context.getWorkflowExecutionId(), getPhaseServiceId(context), queries, finalIndices,
          context.renderExpression(hostnameField), context.renderExpression(messageField), timestampField,
          timestampFieldFormat, logCollectionStartTimeStamp, 0, Integer.parseInt(timeDuration), hostBatch,
          secretManager.getEncryptionDetails(elkConfig, context.getAppId(), context.getWorkflowExecutionId()));

      String waitId = generateUuid();
      delegateTasks.add(aDelegateTask()
                            .withTaskType(TaskType.ELK_COLLECT_LOG_DATA)
                            .withAccountId(accountId)
                            .withAppId(context.getAppId())
                            .withWaitId(waitId)
                            .withParameters(new Object[] {dataCollectionInfo})
                            .withEnvId(envId)
                            .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 5))
                            .build());
      waitIds[i++] = waitId;
    }

    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, true), waitIds);
    List<String> delegateTaskIds = new ArrayList<>();
    for (DelegateTask task : delegateTasks) {
      delegateTaskIds.add(delegateService.queueTask(task));
    }
    return StringUtils.join(delegateTaskIds, ",");
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
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    try {
      new ElkLogFetchRequest(query, "logstash-*", "beat.hostname", "message", "@timestamp",
          Sets.newHashSet("ip-172-31-8-144", "ip-172-31-12-79", "ip-172-31-13-153"),
          1518724315175L - TimeUnit.MINUTES.toMillis(1), 1518724315175L)
          .toElasticSearchJsonObject();
    } catch (Exception ex) {
      invalidFields.put("query", ex.getMessage());
    }
    return invalidFields;
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }

  protected String getTimestampFieldFormat(
      String accountId, String analysisServerConfigId, String indices, String timestampField) {
    try {
      Map<String, ElkIndexTemplate> indexTemplateMap = elkAnalysisService.getIndices(accountId, analysisServerConfigId);
      final ElkIndexTemplate indexTemplate = indexTemplateMap.get(indices);
      Preconditions.checkNotNull(indexTemplate, "No index template mapping found for " + indices);

      final Object timeStampObject = indexTemplate.getProperties().get(timestampField);
      if (timeStampObject == null) {
        logger.warn("No timestamp field mapping for {} for index {} ", timestampField, indices);
        return DEFAULT_TIME_FORMAT;
      }

      JSONObject timeStampJsonObject = new JSONObject(JsonUtils.asJson(timeStampObject));

      if (!timeStampJsonObject.has("format")) {
        return DEFAULT_TIME_FORMAT;
      }
      return timeStampJsonObject.getString("format");
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  @Override
  public Map<String, String> parentTemplateFields(String fieldName) {
    Map<String, String> parentTemplateFields = new LinkedHashMap<>();
    if (fieldName.equals("indices")) {
      if (!configIdTemplatized()) {
        parentTemplateFields.put("analysisServerConfigId", analysisServerConfigId);
      }
    }
    return parentTemplateFields;
  }

  private boolean configIdTemplatized() {
    return TemplateExpressionProcessor.checkFieldTemplatized("analysisServerConfigId", getTemplateExpressions());
  }
}
