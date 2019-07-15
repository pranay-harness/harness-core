package software.wings.sm.states.collaboration;

import static software.wings.beans.TaskType.SERVICENOW_ASYNC;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutput;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.Activity.Type;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class ServiceNowCreateUpdateState extends State implements SweepingOutputStateMixin {
  @Getter @Setter private SweepingOutput.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;
  @Getter @Setter private ServiceNowCreateUpdateParams serviceNowCreateUpdateParams;
  @Inject private transient ActivityService activityService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient SecretManager secretManager;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private transient ActivityHelperService activityHelperService;

  private static final long ASYNC_TASK_TIMEOUT_MILLIS = 60 * 1000;
  private static final String ISSUE_NUMBER = "issueNumber";
  private static final String ISSUE_ID = "issueId";
  private static final String TRANSFORMATION_VALUES = "transformationValues";
  public ServiceNowCreateUpdateState(String name) {
    super(name, StateType.SERVICENOW_CREATE_UPDATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId =
        activityHelperService.createAndSaveActivity(context, Type.Command, getName(), getStateType(), Arrays.asList())
            .getUuid();
    return executeInternal(context, activityId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();

    ServiceNowExecutionData snowExecutionData = (ServiceNowExecutionData) responseEntry.getValue();
    snowExecutionData.setActivityId(activityId);

    if (snowExecutionData.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      Map<String, Object> sweepingOutputMap = new HashMap<>();
      sweepingOutputMap.put(ISSUE_ID, snowExecutionData.getIssueId());
      sweepingOutputMap.put(ISSUE_NUMBER, snowExecutionData.getIssueNumber());
      if (EmptyPredicate.isNotEmpty(snowExecutionData.getTransformationValues())) {
        sweepingOutputMap.put(TRANSFORMATION_VALUES, snowExecutionData.getTransformationValues());
      }
      handleSweepingOutput(sweepingOutputService, context, sweepingOutputMap);
    }

    return anExecutionResponse()
        .withStateExecutionData(snowExecutionData)
        .withExecutionStatus(snowExecutionData.getExecutionStatus())
        .withErrorMessage(snowExecutionData.getResponseMsg())
        .build();
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    ServiceNowConfig config = getSnowConfig(serviceNowCreateUpdateParams.getSnowConnectorId());
    renderExpressions(context, serviceNowCreateUpdateParams);

    String accountId = executionContext.getApp().getAccountId();
    ServiceNowTaskParameters serviceNowTaskParameters;
    if (serviceNowCreateUpdateParams.getAction().equals(ServiceNowAction.IMPORT_SET)) {
      if (!isJSONValid(serviceNowCreateUpdateParams.getJsonBody())) {
        throw new WingsException(
            "Json Body is not a valid Json: " + serviceNowCreateUpdateParams.getJsonBody(), WingsException.USER);
      }
      serviceNowTaskParameters = ServiceNowTaskParameters.builder()
                                     .serviceNowConfig(config)
                                     .encryptionDetails(secretManager.getEncryptionDetails(config,
                                         executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
                                     .action(serviceNowCreateUpdateParams.getAction())
                                     .accountId(accountId)
                                     .importSetTableName(serviceNowCreateUpdateParams.getImportSetTableName())
                                     .jsonBody(serviceNowCreateUpdateParams.getJsonBody())
                                     .build();
    } else {
      serviceNowTaskParameters =
          ServiceNowTaskParameters.builder()
              .serviceNowConfig(config)
              .encryptionDetails(secretManager.getEncryptionDetails(
                  config, executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
              .ticketType(ServiceNowTicketType.valueOf(serviceNowCreateUpdateParams.getTicketType()))
              .issueNumber(serviceNowCreateUpdateParams.getIssueNumber())
              .issueId(serviceNowCreateUpdateParams.getTicketId())
              .action(serviceNowCreateUpdateParams.getAction())
              .updateMultiple(serviceNowCreateUpdateParams.isUpdateMultiple())
              .accountId(accountId)
              .fields(serviceNowCreateUpdateParams.fetchFields())
              .additionalFields(serviceNowCreateUpdateParams.fetchAdditionalFields())
              .build();
    }

    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(accountId)
                                    .waitId(activityId)
                                    .appId(executionContext.getApp().getAppId())
                                    .data(TaskData.builder()
                                              .taskType(SERVICENOW_ASYNC.name())
                                              .parameters(new Object[] {serviceNowTaskParameters})
                                              .timeout(ASYNC_TASK_TIMEOUT_MILLIS)
                                              .build())
                                    .build();
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ServiceNowExecutionData.builder().activityId(activityId).build())
        .build();
  }

  private void renderExpressions(ExecutionContext context, ServiceNowCreateUpdateParams params) {
    params.setIssueNumber(context.renderExpression(params.getIssueNumber()));
    Map<ServiceNowFields, String> renderedFields = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(params.fetchFields())) {
      for (Entry<ServiceNowFields, String> entry : params.fetchFields().entrySet()) {
        renderedFields.put(entry.getKey(), context.renderExpression(entry.getValue()));
      }
    }
    params.setFields(renderedFields);
    Map<String, String> renderedAdditionalFields = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(params.fetchAdditionalFields())) {
      for (Entry<String, String> entry : params.fetchAdditionalFields().entrySet()) {
        renderedAdditionalFields.put(entry.getKey(), context.renderExpression(entry.getValue()));
      }
    }

    params.setAdditionalFields(renderedAdditionalFields);
  }

  private ServiceNowConfig getSnowConfig(String snowConnectorId) {
    SettingAttribute snowSettingAttribute = wingsPersistence.get(SettingAttribute.class, snowConnectorId);
    Validator.notNullCheck("snowSettingAttribute", snowSettingAttribute);

    if (!(snowSettingAttribute.getValue() instanceof ServiceNowConfig)) {
      throw new InvalidRequestException("Type of Setting Attribute Value is not SnowConfig");
    }

    return (ServiceNowConfig) snowSettingAttribute.getValue();
  }

  public boolean isJSONValid(String test) {
    try {
      new JSONObject(test);
    } catch (JSONException ex) {
      try {
        new JSONArray(test);
      } catch (JSONException ex1) {
        return false;
      }
    }
    return true;
  }
}
