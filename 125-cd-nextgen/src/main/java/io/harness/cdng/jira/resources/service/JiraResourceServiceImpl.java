package io.harness.cdng.jira.resources.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.converter.JiraFieldDTOConverter;
import io.harness.cdng.jira.resources.converter.JiraIssueDTOConverter;
import io.harness.cdng.jira.resources.converter.JiraIssueTypeDTOConverter;
import io.harness.cdng.jira.resources.converter.JiraProjectDTOConverter;
import io.harness.cdng.jira.resources.converter.JiraTaskNgParametersBuilderConverter;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.dto.JiraApprovalDTO;
import io.harness.cdng.jira.resources.response.dto.JiraFieldDTO;
import io.harness.cdng.jira.resources.response.dto.JiraGetCreateMetadataDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;
import io.harness.cdng.jira.resources.response.dto.JiraProjectDTO;
import io.harness.common.NGTaskType;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.apis.dto.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    JiraTaskNGParametersBuilder taskParametersBuilder = JiraTaskNGParameters.builder().jiraAction(JiraAction.AUTH);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectionRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return true;
  }

  @Override
  public String createTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, CreateJiraTicketRequest request) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNgParametersBuilderConverter.toJiraTaskNGParametersBuilderFromCreate.apply(request).jiraAction(
            JiraAction.CREATE_TICKET);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getIssueKey();
  }

  @Override
  public String updateTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, UpdateJiraTicketRequest request) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNgParametersBuilderConverter.toJiraTaskNGParametersBuilderFromUpdate.apply(request).jiraAction(
            JiraAction.UPDATE_TICKET);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);
    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getIssueKey();
  }

  @Override
  public JiraIssueDTO fetchIssue(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String jiraIssueId) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNGParameters.builder().issueId(jiraIssueId).jiraAction(JiraAction.FETCH_ISSUE);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != RUNNING) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return JiraIssueDTOConverter.toJiraIssueDTO().apply(jiraTaskResponse);
  }

  @Override
  public List<JiraProjectDTO> getProjects(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNGParameters.builder().jiraAction(JiraAction.GET_PROJECTS);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);
    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getProjects()
        .stream()
        .map(JiraProjectDTOConverter.toJiraProjectDTO)
        .collect(Collectors.toList());
  }

  @Override
  public List<JiraIssueTypeDTO> getProjectStatuses(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String projectKey) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNGParameters.builder().project(projectKey).jiraAction(JiraAction.GET_STATUSES);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getStatuses()
        .stream()
        .map(JiraIssueTypeDTOConverter.toJiraIssueTypeDTO)
        .collect(Collectors.toList());
  }

  @Override
  public List<JiraFieldDTO> getFieldsOptions(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String projectKey) {
    JiraTaskNGParametersBuilder taskParametersBuilder =
        JiraTaskNGParameters.builder().project(projectKey).jiraAction(JiraAction.GET_FIELDS_OPTIONS);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    return jiraTaskResponse.getFields().stream().map(JiraFieldDTOConverter.toJiraFieldDTO).collect(Collectors.toList());
  }

  @Override
  public JiraApprovalDTO checkApproval(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier,
      String issueId, String approvalField, String approvalFieldValue, String rejectionField,
      String rejectionFieldValue) {
    JiraTaskNGParametersBuilder taskParametersBuilder = JiraTaskNGParameters.builder()
                                                            .jiraAction(JiraAction.CHECK_APPROVAL)
                                                            .issueId(issueId)
                                                            .approvalField(approvalField)
                                                            .approvalValue(approvalFieldValue)
                                                            .rejectionField(rejectionField)
                                                            .rejectionValue(rejectionFieldValue);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    return JiraApprovalDTO.builder()
        .isApproved(jiraTaskResponse.getExecutionStatus() == SUCCESS)
        .status(jiraTaskResponse.getCurrentStatus())
        .build();
  }

  @Override
  public JiraGetCreateMetadataDTO getCreateMetadata(IdentifierRef jiraConnectorRef, String orgIdentifier,
      String projectIdentifier, String projectKey, String createExpandParam) {
    JiraTaskNGParametersBuilder taskParametersBuilder = JiraTaskNGParameters.builder()
                                                            .project(projectKey)
                                                            .createmetaExpandParam(createExpandParam)
                                                            .jiraAction(JiraAction.GET_CREATE_METADATA);

    JiraTaskNGResponse jiraTaskResponse =
        obtainJiraTaskNGResponse(jiraConnectorRef, orgIdentifier, projectIdentifier, taskParametersBuilder);

    if (jiraTaskResponse.getExecutionStatus() != SUCCESS) {
      throw new HarnessJiraException(jiraTaskResponse.getErrorMessage(), EnumSet.of(REST_API));
    }

    JiraCreateMetaResponse createMetadata = jiraTaskResponse.getCreateMetadata();

    return JiraGetCreateMetadataDTO.builder()
        .expand(createMetadata.getExpand())
        .projects(createMetadata.getProjects()
                      .stream()
                      .map(JiraProjectDTOConverter.toJiraProjectDTO)
                      .collect(Collectors.toList()))
        .build();
  }

  private JiraTaskNGResponse obtainJiraTaskNGResponse(IdentifierRef jiraConnectionRef, String orgIdentifier,
      String projectIdentifier, JiraTaskNGParametersBuilder jiraTaskNGParametersBuilder) {
    JiraConnectorDTO connector = getConnector(jiraConnectionRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(jiraConnectionRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);

    JiraTaskNGParameters taskParameters =
        jiraTaskNGParametersBuilder.accountId(jiraConnectionRef.getAccountIdentifier())
            .encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
            .jiraConnectorDTO(connector)
            .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);

    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), EnumSet.of(REST_API));
    }

    return (JiraTaskNGResponse) responseData;
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
        .taskType(NGTaskType.JIRA_TASK_NG.name())
        .taskParameters(taskNGParameters)
        .executionTimeout(Duration.ofSeconds(timeoutInSecs))
        .taskSetupAbstraction("orgIdentifier", baseNGAccess.getOrgIdentifier())
        .taskSetupAbstraction("projectIdentifier", baseNGAccess.getProjectIdentifier())
        .build();
  }
}
