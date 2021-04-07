package io.harness.cdng.jira.resources.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.jira.JiraActionNG;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraIssueUpdateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;
import io.harness.jira.JiraStatusNG;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@OwnedBy(CDC)
@Singleton
public class JiraResourceServiceImpl implements JiraResourceService {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Inject
  public JiraResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService, DelegateGrpcClientWrapper delegateGrpcClientWrapper) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
  }

  @Override
  public boolean validateCredentials(IdentifierRef jiraConnectionRef, String orgId, String projectId) {
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder().action(JiraActionNG.VALIDATE_CREDENTIALS);
    obtainJiraTaskNGResponse(jiraConnectionRef, orgId, projectId, paramsBuilder);
    return true;
  }

  @Override
  public List<JiraProjectBasicNG> getProjects(IdentifierRef jiraConnectorRef, String orgId, String projectId) {
    JiraTaskNGParametersBuilder paramsBuilder = JiraTaskNGParameters.builder().action(JiraActionNG.GET_PROJECTS);
    JiraTaskNGResponse jiraTaskResponse = obtainJiraTaskNGResponse(jiraConnectorRef, orgId, projectId, paramsBuilder);
    return jiraTaskResponse.getProjects();
  }

  @Override
  public List<JiraStatusNG> getStatuses(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String projectKey, String issueType) {
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder().action(JiraActionNG.GET_STATUSES).projectKey(projectKey).issueType(issueType);
    JiraTaskNGResponse jiraTaskResponse = obtainJiraTaskNGResponse(jiraConnectorRef, orgId, projectId, paramsBuilder);
    return jiraTaskResponse.getStatuses();
  }

  @Override
  public JiraIssueCreateMetadataNG getIssueCreateMetadata(IdentifierRef jiraConnectorRef, String orgId,
      String projectId, String projectKey, String issueType, String expand, boolean fetchStatus) {
    JiraTaskNGParametersBuilder paramsBuilder = JiraTaskNGParameters.builder()
                                                    .action(JiraActionNG.GET_ISSUE_CREATE_METADATA)
                                                    .projectKey(projectKey)
                                                    .issueType(issueType)
                                                    .expand(expand)
                                                    .fetchStatus(fetchStatus);
    JiraTaskNGResponse jiraTaskResponse = obtainJiraTaskNGResponse(jiraConnectorRef, orgId, projectId, paramsBuilder);
    return jiraTaskResponse.getIssueCreateMetadata();
  }

  @Override
  public JiraIssueUpdateMetadataNG getIssueUpdateMetadata(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String issueKey) {
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder().action(JiraActionNG.GET_ISSUE_UPDATE_METADATA).issueKey(issueKey);
    JiraTaskNGResponse jiraTaskResponse = obtainJiraTaskNGResponse(jiraConnectorRef, orgId, projectId, paramsBuilder);
    return jiraTaskResponse.getIssueUpdateMetadata();
  }

  private JiraTaskNGResponse obtainJiraTaskNGResponse(
      IdentifierRef jiraConnectionRef, String orgId, String projectId, JiraTaskNGParametersBuilder paramsBuilder) {
    JiraConnectorDTO connector = getConnector(jiraConnectionRef);
    BaseNGAccess baseNGAccess = getBaseNGAccess(jiraConnectionRef, orgId, projectId);
    JiraTaskNGParameters taskParameters = paramsBuilder.encryptionDetails(getEncryptionDetails(connector, baseNGAccess))
                                              .jiraConnectorDTO(connector)
                                              .build();

    final DelegateTaskRequest delegateTaskRequest = createDelegateTaskRequest(baseNGAccess, taskParameters);
    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new HarnessJiraException(errorNotifyResponseData.getErrorMessage(), WingsException.USER);
    }

    return (JiraTaskNGResponse) responseData;
  }

  private JiraConnectorDTO getConnector(IdentifierRef jiraConnectorRef) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(jiraConnectorRef.getAccountIdentifier(),
        jiraConnectorRef.getOrgIdentifier(), jiraConnectorRef.getProjectIdentifier(), jiraConnectorRef.getIdentifier());
    if (!connectorDTO.isPresent() || !isJiraConnector(connectorDTO.get())) {
      throw new InvalidRequestException(String.format("Jira connector not found for identifier : [%s] with scope: [%s]",
                                            jiraConnectorRef.getIdentifier(), jiraConnectorRef.getScope()),
          WingsException.USER);
    }

    ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
    return (JiraConnectorDTO) connectors.getConnectorConfig();
  }

  private BaseNGAccess getBaseNGAccess(IdentifierRef ref, String orgId, String projectId) {
    return BaseNGAccess.builder()
        .accountIdentifier(ref.getAccountIdentifier())
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .build();
  }

  private boolean isJiraConnector(ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.JIRA == connectorResponseDTO.getConnector().getConnectorType();
  }

  private List<EncryptedDataDetail> getEncryptionDetails(JiraConnectorDTO jiraConnectorDTO, NGAccess ngAccess) {
    return secretManagerClientService.getEncryptionDetails(ngAccess, jiraConnectorDTO);
  }

  private DelegateTaskRequest createDelegateTaskRequest(
      BaseNGAccess baseNGAccess, JiraTaskNGParameters taskNGParameters) {
    return DelegateTaskRequest.builder()
        .accountId(baseNGAccess.getAccountIdentifier())
        .taskType(NGTaskType.JIRA_TASK_NG.name())
        .taskParameters(taskNGParameters)
        .taskSelectors(taskNGParameters.getDelegateSelectors())
        .executionTimeout(TIMEOUT)
        .taskSetupAbstraction(SetupAbstractionKeys.orgIdentifier, baseNGAccess.getOrgIdentifier())
        .taskSetupAbstraction(SetupAbstractionKeys.projectIdentifier, baseNGAccess.getProjectIdentifier())
        .build();
  }
}
