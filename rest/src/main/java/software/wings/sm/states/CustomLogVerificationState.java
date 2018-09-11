package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.time.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Praveen
 */
public class CustomLogVerificationState extends AbstractLogAnalysisState {
  public CustomLogVerificationState(String name) {
    super(name, StateType.LOG_VERIFICATION.getType());
  }

  @Transient
  @SchemaIgnore
  private static final Logger logger = LoggerFactory.getLogger(CustomLogVerificationState.class);

  private List<LogCollectionInfo> logCollectionInfos;

  public void setLogCollectionInfos(List<LogCollectionInfo> collectionInfos) {
    this.logCollectionInfos = collectionInfos;
  }

  public List<LogCollectionInfo> getLogCollectionInfos() {
    return this.logCollectionInfos;
  }

  @Attributes(required = true, title = "Log Server") private String analysisServerConfigId;

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Attributes(required = false, title = "Log DataCollection Rate (mins)") private int dataCollectionRate;

  public int getDataCollectionRate() {
    return dataCollectionRate < 1 ? 1 : dataCollectionRate;
  }

  public void setDataCollectionRate(int dataCollectionRate) {
    this.dataCollectionRate = dataCollectionRate;
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

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, LogAnalysisExecutionData data, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = null;
    String serverConfigId = analysisServerConfigId;
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        serverConfigId = settingAttribute.getUuid();
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(serverConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No custom log setting with id: " + analysisServerConfigId + " found");
      }
    }

    final APMVerificationConfig logConfig = (APMVerificationConfig) settingAttribute.getValue();
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    String accountId = appService.get(context.getAppId()).getAccountId();

    CustomLogDataCollectionInfo dataCollectionInfo =
        CustomLogDataCollectionInfo.builder()
            .baseUrl(logConfig.getUrl())
            .validationUrl(logConfig.getValidationUrl())
            .headers(logConfig.collectionHeaders())
            .options(logConfig.collectionParams())
            .encryptedDataDetails(logConfig.encryptedDataDetails(secretManager))
            .hosts(hosts)
            .stateType(StateType.LOG_VERIFICATION)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .startMinute(0)
            .responseDefinition(constructLogDefinitions(context))
            .collectionFrequency(getDataCollectionRate())
            .collectionTime(Integer.parseInt(timeDuration))
            .accountId(accountId)
            .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.CUSTOM_LOG_COLLECTION_TASK)
                                    .withAccountId(accountId)
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), data, false), waitId);
    return delegateService.queueTask(delegateTask);
  }

  protected Map<String, Map<String, ResponseMapper>> constructLogDefinitions(final ExecutionContext context) {
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    for (LogCollectionInfo logInfo : logCollectionInfos) {
      String evaluatedUrl = context.renderExpression(logInfo.getCollectionUrl());
      logDefinition.put(evaluatedUrl, getResponseMappers(logInfo));
    }
    return logDefinition;
  }

  private Map<String, ResponseMapper> getResponseMappers(LogCollectionInfo logCollectionInfo) {
    ResponseMapping responseMapping = logCollectionInfo.getResponseMapping();
    Map<String, ResponseMapper> responseMappers = new HashMap<>();

    // Set the host details (if exists) in the responseMapper
    if (!isEmpty(responseMapping.getHostJsonPath())) {
      String hostJson = responseMapping.getHostJsonPath();
      List<String> hostRegex =
          isEmpty(responseMapping.getHostRegex()) ? null : Lists.newArrayList(responseMapping.getHostRegex());
      ResponseMapper hostResponseMapper =
          ResponseMapper.builder().fieldName("host").regexs(hostRegex).jsonPath(Arrays.asList(hostJson)).build();
      responseMappers.put("host", hostResponseMapper);
    }
    responseMappers.put("timestamp",
        ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList(responseMapping.getTimestampJsonPath()))
            .timestampFormat(responseMapping.getTimestampFormat())
            .build());

    responseMappers.put("logMessage",
        ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList(responseMapping.getLogMessageJsonPath()))
            .build());
    // TODO: Need to put in the timestamp format as well.

    return responseMappers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LogCollectionInfo {
    private String collectionUrl;
    private ResponseType responseType;
    private CustomLogVerificationState.ResponseMapping responseMapping;
    private Method method;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResponseMapping {
    private String logMessageJsonPath;
    private String hostJsonPath;
    private String hostRegex;
    private String timestampJsonPath;
    private String timestampFormat;
  }
  public enum ResponseType { JSON }

  public enum Method { POST, GET }

  @Data
  @Builder
  public static class ResponseMapper {
    private String fieldName;
    private String fieldValue;
    private List<String> jsonPath;
    private List<String> regexs;
    private String timestampFormat;
  }
}
