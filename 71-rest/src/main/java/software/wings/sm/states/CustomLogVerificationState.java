package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDER;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Praveen
 */
@Slf4j
public class CustomLogVerificationState extends AbstractLogAnalysisState {
  public CustomLogVerificationState(String name) {
    super(name, StateType.LOG_VERIFICATION.getType());
  }

  private List<LogCollectionInfo> logCollectionInfos;

  public void setLogCollectionInfos(List<LogCollectionInfo> collectionInfos) {
    this.logCollectionInfos = collectionInfos;
    if (isNotEmpty(collectionInfos)) {
      this.query = collectionInfos.get(0).getCollectionUrl();
    }
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

  @Override
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
  @Override
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
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
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(logCollectionInfos)) {
      invalidFields.put("Log Collection Info", "Log collection info should not be empty");
    }
    return invalidFields;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData data, Set<String> hosts) {
    String envId = getEnvId(context);

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
    boolean shouldDoHostbasedFiltering = shouldInspectHostsForLogAnalysis();
    final long dataCollectionStartTimeStamp = dataCollectionStartTimestampMillis();
    String accountId = appService.get(context.getAppId()).getAccountId();

    Map<String, Map<String, ResponseMapper>> logDefinitions = constructLogDefinitions(context, logCollectionInfos);
    CustomLogDataCollectionInfo dataCollectionInfo =
        CustomLogDataCollectionInfo.builder()
            .baseUrl(logConfig.getUrl())
            .validationUrl(logConfig.getValidationUrl())
            .headers(logConfig.collectionHeaders())
            .options(logConfig.collectionParams())
            .encryptedDataDetails(logConfig.encryptedDataDetails(secretManager))
            .hosts(hosts)
            .query(getRenderedQuery())
            .stateType(StateType.LOG_VERIFICATION)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .startMinute(0)
            .responseDefinition(logDefinitions)
            .collectionFrequency(getDataCollectionRate())
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .accountId(accountId)
            .shouldDoHostBasedFiltering(shouldDoHostbasedFiltering)
            .build();

    String waitId = generateUuid();
    String infrastructureMappingId = context.fetchInfraMappingId();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .accountId(accountId)
            .appId(context.getAppId())
            .waitId(waitId)
            .data(TaskData.builder()
                      .taskType(TaskType.CUSTOM_LOG_COLLECTION_TASK.name())
                      .parameters(new Object[] {dataCollectionInfo})
                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 120))
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
            .executionData(data)
            .build(),
        waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  protected boolean shouldInspectHostsForLogAnalysis() {
    boolean shouldDoHostbasedFiltering = true;
    if (getComparisonStrategy().equals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)) {
      shouldDoHostbasedFiltering = logCollectionInfos.stream().allMatch(logCollectionInfo
          -> logCollectionInfo.getCollectionUrl().contains(VERIFICATION_HOST_PLACEHOLDER)
              || (logCollectionInfo.getCollectionBody() != null
                     && logCollectionInfo.getCollectionBody().contains(VERIFICATION_HOST_PLACEHOLDER)));
    }
    return shouldDoHostbasedFiltering;
  }

  public static Map<String, Map<String, ResponseMapper>> constructLogDefinitions(
      final ExecutionContext context, final List<LogCollectionInfo> logCollectionInfos) {
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    for (LogCollectionInfo logInfo : logCollectionInfos) {
      String evaluatedUrl =
          context != null ? context.renderExpression(logInfo.getCollectionUrl()) : logInfo.getCollectionUrl();
      logDefinition.put(evaluatedUrl, getResponseMappers(logInfo));
    }
    return logDefinition;
  }

  private static Map<String, ResponseMapper> getResponseMappers(LogCollectionInfo logCollectionInfo) {
    ResponseMapping responseMapping = logCollectionInfo.getResponseMapping();
    Map<String, ResponseMapper> responseMappers = new HashMap<>();

    // Set the host details (if exists) in the responseMapper
    if (!isEmpty(responseMapping.getHostJsonPath())) {
      List hostJsonList = new ArrayList();
      String hostJson = responseMapping.getHostJsonPath();
      hostJsonList.add(hostJson);
      List<String> hostRegex =
          isEmpty(responseMapping.getHostRegex()) ? null : Lists.newArrayList(responseMapping.getHostRegex());
      ResponseMapper hostResponseMapper =
          ResponseMapper.builder().fieldName("host").regexs(hostRegex).jsonPath(hostJsonList).build();
      responseMappers.put("host", hostResponseMapper);
    }
    List timestampJsonList = new ArrayList(), logMsgList = new ArrayList();
    timestampJsonList.add(responseMapping.getTimestampJsonPath());
    responseMappers.put("timestamp",
        ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(timestampJsonList)
            .timestampFormat(responseMapping.getTimestampFormat())
            .build());

    logMsgList.add(responseMapping.getLogMessageJsonPath());
    responseMappers.put("logMessage", ResponseMapper.builder().fieldName("logMessage").jsonPath(logMsgList).build());

    return responseMappers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LogCollectionInfo {
    private String collectionUrl;
    private String collectionBody;
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
