package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.api.PhaseElement;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicSettingProvider;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.*;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.NEW_RELIC_DEPLOYMENT_MARKER;

@Attributes
public class NewRelicDeploymentMarkerState extends State {
  @Transient
  @SchemaIgnore
  private static final Logger logger = LoggerFactory.getLogger(NewRelicDeploymentMarkerState.class);

  @Transient @Inject private DelegateService delegateService;
  @Transient @Inject protected SettingsService settingsService;

  @Inject @Transient protected SecretManager secretManager;

  @Transient @Inject protected WaitNotifyEngine waitNotifyEngine;

  @EnumData(enumDataProvider = NewRelicSettingProvider.class)
  @Attributes(required = true, title = "New Relic Server")
  private String analysisServerConfigId;

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
  public ExecutionResponse execute(ExecutionContext context) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No new relic setting with id: " + analysisServerConfigId + " found");
    }

    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    String evaluatedBody = context.renderExpression(body);

    final NewRelicDataCollectionInfo dataCollectionInfo =
        NewRelicDataCollectionInfo.builder()
            .newRelicConfig(newRelicConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .newRelicAppId(Long.parseLong(applicationId))
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                newRelicConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .settingAttributeId(analysisServerConfigId)
            .deploymentMarker(evaluatedBody)
            .build();
    // String waitId = UUID.randomUUID().toString();
    String correlationId = UUID.randomUUID().toString();

    String delegateTaksId =
        delegateService.queueTask(aDelegateTask()
                                      .withTaskType(TaskType.NEWRELIC_POST_DEPLOYMENT_MARKER)
                                      .withAccountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                      .withWaitId(correlationId)
                                      .withAppId(((ExecutionContextImpl) context).getApp().getAppId())
                                      .withParameters(new Object[] {dataCollectionInfo})
                                      .withEnvId(envId)
                                      .withInfrastructureMappingId(infrastructureMappingId)
                                      .build());

    // waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, false), waitId);

    final MetricAnalysisExecutionData executionData =
        MetricAnalysisExecutionData.builder()
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .serverConfigId(getAnalysisServerConfigId())
            .correlationId(correlationId)
            .build();
    executionData.setStatus(ExecutionStatus.RUNNING);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(correlationId))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("Sending deployment marker to NewRelic")
        .withStateExecutionData(executionData)
        .withDelegateTaskId(delegateTaksId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    DataCollectionTaskResult executionResponse = (DataCollectionTaskResult) response.values().iterator().next();
    if (executionResponse.getStatus() == DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withStateExecutionData(StateExecutionData.StateExecutionDataBuilder.aStateExecutionData()
                                      .withStatus(ExecutionStatus.FAILED)
                                      .withErrorMsg(executionResponse.getErrorMessage())
                                      .build())
          .withErrorMessage(executionResponse.getErrorMessage())
          .build();
    }

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(StateExecutionData.StateExecutionDataBuilder.aStateExecutionData()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .build())
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
}
