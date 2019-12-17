package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.elk.ElkQueryTypeProvider;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by raghu on 8/4/17.
 */
@Slf4j
public class ElkAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient public static final String DEFAULT_TIME_FIELD = "@timestamp";

  @SchemaIgnore @Transient protected static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  @Transient @Inject protected ElkAnalysisService elkAnalysisService;

  @Attributes(required = true, title = "Elastic Search Server") protected String analysisServerConfigId;

  @Attributes(title = "Elastic search indices to search", required = true)
  @DefaultValue("_all")
  protected String indices;

  @Attributes(required = true, title = "Timestamp Field")
  @DefaultValue(DEFAULT_TIME_FIELD)
  protected String timestampField;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  @Attributes(required = true, title = "Timestamp format")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  private String timestampFormat;

  @Attributes(required = true, title = "Query Type") @DefaultValue("TERM") private String queryType;

  @Override
  public void setQuery(String query) {
    this.query = query.trim();
  }

  @Attributes(
      required = true, title = "Search Keywords", description = "Wildcarded queries with '*' can affect cluster health")
  @DefaultValue("error")
  public String
  getQuery() {
    return query;
  }

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

  @DefaultValue("beat.hostname")
  @Attributes(required = true, title = "Hostname or Container Id Field")
  public String getHostnameField() {
    return hostnameField;
  }

  public void setHostnameField(String hostnameField) {
    this.hostnameField = hostnameField;
  }

  public String getTimestampField() {
    if (timestampField == null) {
      return DEFAULT_TIME_FIELD;
    }
    return timestampField;
  }

  public void setTimestampField(String timestampField) {
    this.timestampField = timestampField;
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
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @EnumData(enumDataProvider = ElkQueryTypeProvider.class)
  public ElkQueryType getQueryType() {
    if (isBlank(queryType)) {
      return ElkQueryType.TERM;
    }
    return ElkQueryType.valueOf(queryType);
  }

  @Attributes(required = true, title = "Include nodes from previous phases")
  public boolean getIncludePreviousPhaseNodes() {
    return includePreviousPhaseNodes;
  }

  public void setQueryType(String queryType) {
    this.queryType = queryType;
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

  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts) {
    final String timestampField = getTimestampField();
    final String accountId = appService.get(context.getAppId()).getAccountId();
    String envId = getEnvId(context);

    SettingAttribute settingAttribute = null;
    String finalAnalysisServerConfigId = analysisServerConfigId;
    String finalIndices = indices;

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
      throw new IllegalStateException("No elk setting with id: " + finalAnalysisServerConfigId + " found");
    }

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();

    final String timestampFieldFormat = getTimestampFormat();
    final long logCollectionStartTimeStamp = dataCollectionStartTimestampMillis();

    List<Set<String>> batchedHosts = batchHosts(hosts);
    String[] waitIds = new String[batchedHosts.size()];
    List<DelegateTask> delegateTasks = new ArrayList<>();
    int i = 0;
    for (Set<String> hostBatch : batchedHosts) {
      final ElkDataCollectionInfo dataCollectionInfo =
          ElkDataCollectionInfo.builder()
              .elkConfig(elkConfig)
              .accountId(accountId)
              .applicationId(context.getAppId())
              .stateExecutionId(context.getStateExecutionInstanceId())
              .workflowId(getWorkflowId(context))
              .workflowExecutionId(context.getWorkflowExecutionId())
              .serviceId(getPhaseServiceId(context))
              .query(getRenderedQuery())
              .indices(finalIndices)
              .hostnameField(context.renderExpression(hostnameField))
              .messageField(context.renderExpression(messageField))
              .timestampField(timestampField)
              .timestampFieldFormat(timestampFieldFormat)
              .queryType(getQueryType())
              .startTime(logCollectionStartTimeStamp)
              .startMinute((int) (logCollectionStartTimeStamp / TimeUnit.MINUTES.toMillis(1)))
              .collectionTime(Integer.parseInt(getTimeDuration()))
              .hosts(hostBatch)
              .initialDelayMinutes(DELAY_MINUTES)
              .encryptedDataDetails(
                  secretManager.getEncryptionDetails(elkConfig, context.getAppId(), context.getWorkflowExecutionId()))
              .build();

      String waitId = generateUuid();
      delegateTasks.add(DelegateTask.builder()
                            .async(true)
                            .accountId(accountId)
                            .appId(context.getAppId())
                            .waitId(waitId)
                            .data(TaskData.builder()
                                      .taskType(TaskType.ELK_COLLECT_LOG_DATA.name())
                                      .parameters(new Object[] {dataCollectionInfo})
                                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 5))
                                      .build())
                            .envId(envId)
                            .build());
      waitIds[i++] = waitId;
    }

    waitNotifyEngine.waitForAllOn(GENERAL,
        DataCollectionCallback.builder()
            .appId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .dataCollectionStartTime(logCollectionStartTimeStamp)
            .dataCollectionEndTime(
                logCollectionStartTimeStamp + TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration())))
            .executionData(executionData)
            .build(),
        waitIds);
    List<String> delegateTaskIds = new ArrayList<>();
    for (DelegateTask task : delegateTasks) {
      delegateTaskIds.add(delegateService.queueTask(task));
    }
    return StringUtils.join(delegateTaskIds, ",");
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  /**
   * Validates Query on Manager side. No ELK call is made here.
   *
   * @return
   */
  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    try {
      ElkLogFetchRequest.builder()
          .query(query)
          .indices(indices)
          .hostnameField(hostnameField)
          .messageField(messageField)
          .hosts(Sets.newHashSet("ip-172-31-8-144", "ip-172-31-12-79", "ip-172-31-13-153"))
          .startTime(1518724315175L - TimeUnit.MINUTES.toMillis(1))
          .endTime(1518724315175L)
          .queryType(ElkQueryType.valueOf(queryType))
          .build()
          .toElasticSearchJsonObject();
    } catch (Exception ex) {
      invalidFields.put("query", ExceptionUtils.getMessage(ex));
    }
    return invalidFields;
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
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

  @Override
  public DataCollectionInfoV2 createDataCollectionInfo(ExecutionContext context, Set<String> hosts) {
    String envId = getEnvId(context);
    String finalAnalysisServerConfigId = analysisServerConfigId;
    String finalIndices = indices;

    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        SettingAttribute settingAttribute =
            templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        finalAnalysisServerConfigId = settingAttribute.getUuid();
      }
      TemplateExpression indicesExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "indices");
      if (indicesExpression != null) {
        finalIndices = templateExpressionProcessor.resolveTemplateExpression(context, indicesExpression);
      }
    }

    return ElkDataCollectionInfoV2.builder()
        .connectorId(finalAnalysisServerConfigId)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .stateExecutionId(context.getStateExecutionInstanceId())
        .workflowId(context.getWorkflowId())
        .accountId(appService.get(context.getAppId()).getAccountId())
        .envId(envId)
        .applicationId(context.getAppId())
        .query(getRenderedQuery())
        .hostnameField(getHostnameField())
        .hosts(hosts)
        .indices(finalIndices)
        .messageField(messageField)
        .timestampField(getTimestampField())
        .timestampFieldFormat(getTimestampFormat())
        .queryType(getQueryType())
        .build();
  }

  @Override
  protected Optional<FeatureName> getCVTaskFeatureName() {
    return Optional.of(FeatureName.ELK_CV_TASK);
  }
}
