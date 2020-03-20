package software.wings.sm.states;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static java.util.Arrays.asList;
import static software.wings.common.TemplateExpressionProcessor.checkFieldTemplatized;
import static software.wings.sm.StateType.NEW_RELIC_DEPLOYMENT_MARKER;
import static software.wings.utils.Misc.isLong;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMarkerExecutionData;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Attributes
@Slf4j
@FieldNameConstants(innerTypeName = "NewRelicDeploymentMarkerStateKeys")
public class NewRelicDeploymentMarkerState extends AbstractAnalysisState {
  @Inject private NewRelicService newRelicService;

  @Attributes(required = true, title = "New Relic Server") private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  @Attributes(title = "Body") private String body;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public NewRelicDeploymentMarkerState(String name) {
    super(name, NEW_RELIC_DEPLOYMENT_MARKER.name());
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    return asList(body);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String finalServerConfigId = getResolvedConnectorId(
        context, NewRelicDeploymentMarkerStateKeys.analysisServerConfigId, analysisServerConfigId);
    String finalNewRelicApplicationId =
        getResolvedFieldValue(context, NewRelicDeploymentMarkerStateKeys.applicationId, applicationId);

    if (!isLong(finalNewRelicApplicationId)) {
      finalNewRelicApplicationId = renderApplicationExpression(finalNewRelicApplicationId, finalServerConfigId);
    }
    SettingAttribute settingAttribute = getSettingAttribute(finalServerConfigId);

    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    String evaluatedBody = context.renderExpression(body);

    final NewRelicDataCollectionInfo dataCollectionInfo =
        NewRelicDataCollectionInfo.builder()
            .newRelicConfig(newRelicConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .newRelicAppId(Long.parseLong(finalNewRelicApplicationId))
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                newRelicConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .settingAttributeId(finalServerConfigId)
            .deploymentMarker(evaluatedBody)
            .build();
    String correlationId = UUID.randomUUID().toString();

    String delegateTaskId =
        delegateService.queueTask(DelegateTask.builder()
                                      .async(true)
                                      .accountId(context.getApp().getAccountId())
                                      .waitId(correlationId)
                                      .appId(context.getApp().getAppId())
                                      .data(TaskData.builder()
                                                .taskType(TaskType.NEWRELIC_POST_DEPLOYMENT_MARKER.name())
                                                .parameters(new Object[] {dataCollectionInfo})
                                                .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                                .build())
                                      .build());

    final NewRelicMarkerExecutionData executionData =
        NewRelicMarkerExecutionData.builder().payload(body).evaluatedPayload(evaluatedBody).build();
    executionData.setStatus(ExecutionStatus.RUNNING);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(correlationId))
        .executionStatus(ExecutionStatus.RUNNING)
        .errorMessage("Sending deployment marker to NewRelic")
        .stateExecutionData(executionData)
        .delegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    DataCollectionTaskResult executionResponse = (DataCollectionTaskResult) response.values().iterator().next();
    NewRelicMarkerExecutionData analysisExecutionData =
        NewRelicMarkerExecutionData.builder()
            .payload(body)
            .evaluatedPayload(executionResponse.getNewRelicDeploymentMarkerBody())
            .build();
    if (executionResponse.getStatus() == DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE) {
      analysisExecutionData.setErrorMsg(executionResponse.getErrorMessage());
      analysisExecutionData.setStatus(ExecutionStatus.FAILED);

      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .stateExecutionData(analysisExecutionData)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }

    analysisExecutionData.setStatus(ExecutionStatus.SUCCESS);
    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(analysisExecutionData)

        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getBody() {
    return body;
  }

  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public void setBody(String body) {
    this.body = body;
  }

  @Override
  public Map<String, String> parentTemplateFields(String fieldName) {
    Map<String, String> parentTemplateFields = new LinkedHashMap<>();
    if (fieldName.equals(NewRelicDeploymentMarkerStateKeys.applicationId)) {
      if (!checkFieldTemplatized(NewRelicDeploymentMarkerStateKeys.analysisServerConfigId, getTemplateExpressions())) {
        parentTemplateFields.put(NewRelicDeploymentMarkerStateKeys.analysisServerConfigId, analysisServerConfigId);
      }
    }
    return parentTemplateFields;
  }

  private String renderApplicationExpression(String applicationName, String finalServerConfigId) {
    final NewRelicApplication newRelicApplication =
        newRelicService.resolveApplicationName(finalServerConfigId, applicationName);
    return String.valueOf(newRelicApplication.getId());
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message) {
    throw new UnsupportedOperationException();
  }
}
