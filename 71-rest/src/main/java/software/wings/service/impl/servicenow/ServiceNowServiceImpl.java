package software.wings.service.impl.servicenow;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.ApprovalUtils.checkApproval;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ServiceNowException;
import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ServiceNowServiceImpl implements ServiceNowService {
  @Inject @Transient private transient SecretManager secretManager;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WaitNotifyEngine waitNotifyEngine;
  @Inject StateExecutionService stateExecutionService;
  @Inject FeatureFlagService featureFlagService;

  private static final String WORKFLOW_EXECUTION_ID = "workflow-execution-id";
  private static final String APP_ID = "app-id";

  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject SettingsService settingService;

  public enum ServiceNowTicketType {
    INCIDENT("Incident"),
    PROBLEM("Problem"),
    CHANGE_REQUEST("Change"),
    CHANGE_TASK("Change Task");
    @Getter private String displayName;
    ServiceNowTicketType(String s) {
      displayName = s;
    }
  }

  @Data
  @Builder
  public static class ServiceNowMetaDTO {
    private String id;
    private String displayName;
  }

  @Override
  public void validateCredential(SettingAttribute settingAttribute) {
    SyncTaskContext snowTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();

    ServiceNowConfig serviceNowConfig = (ServiceNowConfig) settingAttribute.getValue();
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .serviceNowConfig(serviceNowConfig)
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, APP_ID, WORKFLOW_EXECUTION_ID))
            .build();
    delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).validateConnector(taskParameters);
  }

  @Override
  public List<ServiceNowMetaDTO> getStates(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowTaskParameters taskParameters = getServiceNowTaskParameters(ticketType, accountId, connectorId, appId);

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getStates(taskParameters);
  }

  public List<ServiceNowMetaDTO> getApprovalValues(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowTaskParameters taskParameters = getServiceNowTaskParameters(ticketType, accountId, connectorId, appId);

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getApprovalStates(taskParameters);
  }

  private ServiceNowTaskParameters getServiceNowTaskParameters(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", connectorId);
      throw new ServiceNowException(
          "Error getting ServiceNow connector " + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }

    return ServiceNowTaskParameters.builder()
        .accountId(accountId)
        .ticketType(ticketType)
        .serviceNowConfig(serviceNowConfig)
        .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
        .build();
  }

  @Override
  public Map<String, List<ServiceNowMetaDTO>> getCreateMeta(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", connectorId);
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .accountId(accountId)
            .ticketType(ticketType)
            .serviceNowConfig(serviceNowConfig)
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();
    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getCreateMeta(taskParameters);
  }

  @Override
  public List<ServiceNowMetaDTO> getAdditionalFields(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", connectorId);
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .accountId(accountId)
            .ticketType(ticketType)
            .serviceNowConfig(serviceNowConfig)
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();
    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext)
        .getAdditionalFields(taskParameters);
  }

  @Override
  public ServiceNowExecutionData getIssueUrl(String appId, String accountId, ServiceNowApprovalParams approvalParams) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(approvalParams.getSnowConnectorId()).getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", approvalParams.getSnowConnectorId());
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }

    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .ticketType(approvalParams.getTicketType())
            .issueNumber(approvalParams.getIssueNumber())
            .serviceNowConfig(serviceNowConfig)
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext)
        .getIssueUrl(taskParameters, approvalParams);
  }

  // Todo: Cleanup this method after about 6 months. Keeping it for older approval jobs only.
  @Override
  public Map<String, String> getIssueStatus(ServiceNowApprovalParams approvalParams, String accountId, String appId) {
    ServiceNowConfig serviceNowConfig;
    try {
      SettingAttribute snowConnector = settingService.get(approvalParams.getSnowConnectorId());
      if (snowConnector == null) {
        throw new ServiceNowException(
            "Error getting ServiceNow connector for connector id: " + approvalParams.getSnowConnectorId(),
            SERVICENOW_ERROR, USER);
      }
      serviceNowConfig = (ServiceNowConfig) snowConnector.getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", approvalParams.getSnowConnectorId());
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .accountId(accountId)
            .issueNumber(approvalParams.getIssueNumber())
            .serviceNowConfig(serviceNowConfig)
            .ticketType(approvalParams.getTicketType())
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    // Considering only approval field on the assumption that both approval and rejection fields are equal.
    // Would need to pass rejection field too if we decide to support different fields in the future.
    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext)
        .getIssueStatus(taskParameters, approvalParams.getAllCriteriaFields());
  }

  @Override
  public ServiceNowExecutionData getApprovalStatus(ApprovalPollingJobEntity approvalPollingJobEntity) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(approvalPollingJobEntity.getConnectorId()).getValue();
    } catch (Exception e) {
      logger.error("Error getting ServiceNow connector for ID: {}", approvalPollingJobEntity.getConnectorId());
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .accountId(approvalPollingJobEntity.getAccountId())
            .issueNumber(approvalPollingJobEntity.getIssueNumber())
            .serviceNowConfig(serviceNowConfig)
            .ticketType(approvalPollingJobEntity.getIssueType())
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, approvalPollingJobEntity.getAppId(),
                approvalPollingJobEntity.getWorkflowExecutionId()))
            .build();

    SyncTaskContext snowTaskContext = SyncTaskContext.builder()
                                          .accountId(approvalPollingJobEntity.getAccountId())
                                          .appId(approvalPollingJobEntity.getAppId())
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();

    try {
      // Considering only approval field on the assumption that both approval and rejection fields are equal.
      // Would need to pass rejection field too if we decide to support different fields in the future.
      ServiceNowDelegateService serviceNowDelegateService =
          delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext);

      Map<String, String> currentStatus =
          serviceNowDelegateService.getIssueStatus(taskParameters, approvalPollingJobEntity.getAllServiceNowFields());

      String issueStatus =
          currentStatus.entrySet()
              .stream()
              .map(status -> StringUtils.capitalize(status.getKey()) + " is equal to " + status.getValue())
              .collect(Collectors.joining(",\n"));

      if (approvalPollingJobEntity.getApproval() != null
          && approvalPollingJobEntity.getApproval().satisfied(currentStatus)) {
        return ServiceNowExecutionData.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .currentState(issueStatus)
            .build();
      }
      if (approvalPollingJobEntity.getRejection() != null
          && approvalPollingJobEntity.getRejection().satisfied(currentStatus)) {
        return ServiceNowExecutionData.builder()
            .executionStatus(ExecutionStatus.REJECTED)
            .currentState(issueStatus)
            .build();
      }

      if (EmptyPredicate.isNotEmpty(issueStatus)) {
        return ServiceNowExecutionData.builder()
            .executionStatus(ExecutionStatus.PAUSED)
            .currentState(issueStatus)
            .build();
      }

      issueStatus = serviceNowDelegateService.getIssueFieldStatus(taskParameters, "state");

      if (approvalPollingJobEntity.getApprovalValue() != null
          && approvalPollingJobEntity.getApprovalValue().equalsIgnoreCase(issueStatus)) {
        return ServiceNowExecutionData.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .currentState(issueStatus)
            .build();
      }
      if (approvalPollingJobEntity.getRejectionValue() != null
          && approvalPollingJobEntity.getRejectionValue().equalsIgnoreCase(issueStatus)) {
        return ServiceNowExecutionData.builder()
            .executionStatus(ExecutionStatus.REJECTED)
            .currentState(issueStatus)
            .build();
      }
      return ServiceNowExecutionData.builder()
          .executionStatus(ExecutionStatus.PAUSED)
          .currentState(issueStatus)
          .build();
    } catch (WingsException we) {
      return ServiceNowExecutionData.builder().executionStatus(ExecutionStatus.FAILED).build();
    }
  }

  @Override
  public void handleServiceNowPolling(ApprovalPollingJobEntity entity) {
    ServiceNowExecutionData serviceNowExecutionData = null;
    String approvalId = entity.getApprovalId();
    String issueNumber = entity.getIssueNumber();
    String workflowExecutionId = entity.getWorkflowExecutionId();
    String appId = entity.getAppId();
    String stateExecutionInstanceId = entity.getStateExecutionInstanceId();
    try {
      serviceNowExecutionData = getApprovalStatus(entity);
    } catch (Exception ex) {
      logger.error(
          "Error occurred while polling issue status. Continuing to poll next minute. approvalId: {}, workflowExecutionId: {} , issueNumber: {}",
          approvalId, workflowExecutionId, entity.getIssueNumber(), ex);
      return;
    }
    ExecutionStatus issueStatus = serviceNowExecutionData.getExecutionStatus();
    logger.info("Issue: {} Status from SNOW: {} Current Status {} for approvalId: {}, workflowExecutionId: {} ",
        entity.getIssueNumber(), issueStatus, serviceNowExecutionData.getCurrentState(), approvalId,
        workflowExecutionId);

    checkApproval(workflowExecutionService, stateExecutionService, waitNotifyEngine, appId, approvalId, issueNumber,
        workflowExecutionId, stateExecutionInstanceId, serviceNowExecutionData.getCurrentState(),
        serviceNowExecutionData.getErrorMsg(), issueStatus);
  }
}
