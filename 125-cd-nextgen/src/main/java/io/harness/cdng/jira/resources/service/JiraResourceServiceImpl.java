package io.harness.cdng.jira.resources.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.converter.JiraIssueDTOConverter;
import io.harness.cdng.jira.resources.converter.JiraTaskNgParametersBuilderConverter;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.JiraIssueDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.jira.JiraAction;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import software.wings.beans.TaskType;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
public class JiraResourceServiceImpl implements JiraResourceService {
  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @VisibleForTesting static final int timeoutInSecs = 30;

  @Inject
  public JiraResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService, DelegateGrpcClientWrapper delegateGrpcClientWrapper) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
  }

  @Override
  public boolean validateCredentials(IdentifierRef jiraConnectionRef, String orgIdentifier, String projectIdentifier) {
    JiraConnectorDTO connector = getConnector(jiraConnectionRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jiraConnectionRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);

    JiraTaskNGParameters taskParameters = JiraTaskNGParameters.builder()
                                              .accountId(jiraConnectionRef.getAccountIdentifier())
                                              .jiraAction(JiraAction.AUTH)
                                              .encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
                                              .jiraConnectorDTO(connector)
                                              .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), EnumSet.of(REST_API));
    }
    JiraTaskNGResponse jiraTaskResponse = (JiraTaskNGResponse) responseData;
    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return true;
  }

  @Override
  public String createTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, CreateJiraTicketRequest request) {
    JiraConnectorDTO connector = getConnector(jiraConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jiraConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);

    JiraTaskNGParameters taskParameters = JiraTaskNgParametersBuilderConverter.toJiraTaskNGParametersBuilderFromCreate()
                                              .apply(request)
                                              .accountId(jiraConnectorRef.getAccountIdentifier())
                                              .jiraAction(JiraAction.CREATE_TICKET)
                                              .encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
                                              .jiraConnectorDTO(connector)
                                              .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), EnumSet.of(REST_API));
    }
    JiraTaskNGResponse jiraTaskResponse = (JiraTaskNGResponse) responseData;
    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getIssueKey();
  }

  @Override
  public String updateTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, UpdateJiraTicketRequest request) {
    JiraConnectorDTO connector = getConnector(jiraConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jiraConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);

    JiraTaskNGParameters taskParameters = JiraTaskNgParametersBuilderConverter.toJiraTaskNGParametersBuilderFromUpdate()
                                              .apply(request)
                                              .accountId(jiraConnectorRef.getAccountIdentifier())
                                              .jiraAction(JiraAction.UPDATE_TICKET)
                                              .encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
                                              .jiraConnectorDTO(connector)
                                              .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), EnumSet.of(REST_API));
    }
    JiraTaskNGResponse jiraTaskResponse = (JiraTaskNGResponse) responseData;
    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getIssueKey();
  }

  @Override
  public JiraIssueDTO fetchIssue(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String jiraIssueId) {
    JiraConnectorDTO connector = getConnector(jiraConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jiraConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);

    JiraTaskNGParameters taskParameters = JiraTaskNGParameters.builder()
                                              .issueId(jiraIssueId)
                                              .accountId(jiraConnectorRef.getAccountIdentifier())
                                              .jiraAction(JiraAction.FETCH_ISSUE)
                                              .encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
                                              .jiraConnectorDTO(connector)
                                              .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), EnumSet.of(REST_API));
    }
    JiraTaskNGResponse jiraTaskResponse = (JiraTaskNGResponse) responseData;
    if (jiraTaskResponse.getExecutionStatus() != RUNNING) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return JiraIssueDTOConverter.toJiraIssueDTO().apply(jiraTaskResponse);
  }

  private JiraConnectorDTO getConnector(IdentifierRef jiraConnectorRef) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(jiraConnectorRef.getAccountIdentifier(),
        jiraConnectorRef.getOrgIdentifier(), jiraConnectorRef.getProjectIdentifier(), jiraConnectorRef.getIdentifier());

    if (!connectorDTO.isPresent() || !isJiraConnector(connectorDTO.get())) {
      throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                                            jiraConnectorRef.getIdentifier(), jiraConnectorRef.getScope()),
          WingsException.USER);
    }
    ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
    return (JiraConnectorDTO) connectors.getConnectorConfig();
  }

  private BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private boolean isJiraConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.JIRA == (connectorResponseDTO.getConnector().getConnectorType());
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull JiraConnectorDTO jiraConnectorDTO, @Nonnull NGAccess ngAccess) {
    return secretManagerClientService.getEncryptionDetails(ngAccess, jiraConnectorDTO);
  }

  private DelegateTaskRequest createDelegateTaskRequest(
      BaseNGAccess baseNGAccess, JiraTaskNGParameters taskNGParameters) {
    return DelegateTaskRequest.builder()
        .accountId(baseNGAccess.getAccountIdentifier())
        .taskType(TaskType.JIRA_TASK_NG.name())
        .taskParameters(taskNGParameters)
        .executionTimeout(Duration.ofSeconds(timeoutInSecs))
        .taskSetupAbstraction("orgIdentifier", baseNGAccess.getOrgIdentifier())
        .taskSetupAbstraction("projectIdentifier", baseNGAccess.getProjectIdentifier())
        .build();
  }
}
