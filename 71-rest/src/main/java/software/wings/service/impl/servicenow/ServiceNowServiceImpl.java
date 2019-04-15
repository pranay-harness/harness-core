package software.wings.service.impl.servicenow;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.List;

@Singleton
@Slf4j
public class ServiceNowServiceImpl implements ServiceNowService {
  @Inject @Transient private transient SecretManager secretManager;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WaitNotifyEngine waitNotifyEngine;

  private static final String WORKFLOW_EXECUTION_ID = "workflow-execution-id";
  private static final String APP_ID = "app-id";

  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject SettingsService settingService;

  public enum ServiceNowTicketType {
    INCIDENT("Incident"),
    PROBLEM("Problem"),
    CHANGE_REQUEST("Change");
    @Getter private String displayName;
    ServiceNowTicketType(String s) {
      displayName = s;
    }
  }

  @Data
  @Builder
  public static class ServiceNowState {
    private int id;
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

  public List<ServiceNowState> getStates(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting Service now connector for ID: {}", connectorId);
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER)
          .addParam("message", ExceptionUtils.getMessage(e));
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

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getStates(taskParameters);
  }

  public String getIssueUrl(
      String issueNumber, String connectorId, ServiceNowTicketType ticketType, String appId, String accountId) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting Service now connector for ID: {}", connectorId);
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER)
          .addParam("message", ExceptionUtils.getMessage(e));
    }

    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .ticketType(ticketType)
            .issueNumber(issueNumber)
            .serviceNowConfig(serviceNowConfig)
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    return delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getIssueUrl(taskParameters);
  }

  @Override
  public ApprovalDetails.Action getApprovalStatus(String connectorId, String accountId, String appId,
      String issueNumber, String approvalField, String approvalValue, String rejectionField, String rejectionValue,
      String ticketType) {
    ServiceNowConfig serviceNowConfig;
    try {
      serviceNowConfig = (ServiceNowConfig) settingService.get(connectorId).getValue();
    } catch (Exception e) {
      logger.error("Error getting Service now connector for ID: {}", connectorId);
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER)
          .addParam("message", ExceptionUtils.getMessage(e));
    }
    ServiceNowTaskParameters taskParameters =
        ServiceNowTaskParameters.builder()
            .accountId(accountId)
            .issueNumber(issueNumber)
            .action(ServiceNowAction.CHECK_APPROVAL)
            .serviceNowConfig(serviceNowConfig)
            .ticketType(ServiceNowTicketType.valueOf(ticketType))
            .encryptionDetails(secretManager.getEncryptionDetails(serviceNowConfig, appId, WORKFLOW_EXECUTION_ID))
            .build();

    SyncTaskContext snowTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    String issueStatus =
        delegateProxyFactory.get(ServiceNowDelegateService.class, snowTaskContext).getIssueStatus(taskParameters);
    if (approvalValue != null && approvalValue.equalsIgnoreCase(issueStatus)) {
      return Action.APPROVE;
    }
    if (rejectionValue != null && rejectionValue.equalsIgnoreCase(issueStatus)) {
      return Action.REJECT;
    }
    return null;
  }

  public void approveWorkflow(
      Action action, String approvalId, EmbeddedUser user, String appId, String workflowExecutionId) {
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setAction(action);
    approvalDetails.setApprovalId(approvalId);

    if (user != null) {
      approvalDetails.setApprovedBy(user);
    }
    ApprovalStateExecutionData executionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            appId, workflowExecutionId, null, approvalDetails);
    if (action == Action.APPROVE) {
      executionData.setStatus(ExecutionStatus.SUCCESS);
    } else {
      executionData.setStatus(ExecutionStatus.REJECTED);
    }
    executionData.setApprovedOn(System.currentTimeMillis());
    if (user != null) {
      executionData.setApprovedBy(user);
    }
    logger.info("Sending notify for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    waitNotifyEngine.notify(approvalId, executionData);
  }
}
