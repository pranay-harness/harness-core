package io.harness.cdng.jira.resources.service;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.dto.JiraApprovalDTO;
import io.harness.cdng.jira.resources.response.dto.JiraFieldDTO;
import io.harness.cdng.jira.resources.response.dto.JiraGetCreateMetadataDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueDTO;
import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;
import io.harness.cdng.jira.resources.response.dto.JiraProjectDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.apis.dto.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse.JiraIssueData;
import io.harness.encryption.SecretRefData;
import io.harness.exception.HarnessJiraException;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JiraResourceServiceTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String IDENTIFIER = "identifier";

  @Mock ConnectorService connectorService;
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @InjectMocks JiraResourceServiceImpl jiraResourceService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestValidateCredentials() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build());

    boolean isValid = jiraResourceService.validateCredentials(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(isValid).isTrue();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestValidateCredentialsDelegateError() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("Error").build());

    assertThatThrownBy(() -> jiraResourceService.validateCredentials(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestValidateCredentialsNotSuccessStatus() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.FAILURE).errorMessage("Error").build());

    assertThatThrownBy(() -> jiraResourceService.validateCredentials(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCreateTicket() {
    final String issueKey = "CDNG-0000";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).issueKey(issueKey).build());

    String response = jiraResourceService.createTicket(
        identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, CreateJiraTicketRequest.builder().build());
    assertThat(response).isEqualTo(issueKey);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCreateTicketDelegateError() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("Error").build());

    assertThatThrownBy(()
                           -> jiraResourceService.createTicket(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER,
                               CreateJiraTicketRequest.builder().build()))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCreateTicketNotSuccessStatus() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.FAILURE).errorMessage("Error").build());

    assertThatThrownBy(()
                           -> jiraResourceService.createTicket(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER,
                               CreateJiraTicketRequest.builder().build()))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestUpdateTicket() {
    final String issueKey = "CDNG-0000";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).issueKey(issueKey).build());

    String response = jiraResourceService.updateTicket(
        identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, UpdateJiraTicketRequest.builder().build());
    assertThat(response).isEqualTo(issueKey);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFetchIssue() {
    final String issueKey = "CDNG-0000";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.RUNNING)
                        .issueKey(issueKey)
                        .currentStatus("WAITING FOR APPROVAL")
                        .issueUrl("jiraUrl")
                        .jiraIssueData(JiraIssueData.builder().description("").build())
                        .build());

    JiraIssueDTO jiraIssueDTO =
        jiraResourceService.fetchIssue(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, issueKey);

    assertThat(jiraIssueDTO).isNotNull();
    assertThat(jiraIssueDTO.getIssueKey()).isEqualTo(issueKey);
    assertThat(jiraIssueDTO.getExecutionStatus()).isEqualTo(CommandExecutionStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFetchIssueFailure() {
    final String issueKey = "CDNG-0000";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("Error message")
                        .build());

    assertThatThrownBy(
        () -> jiraResourceService.fetchIssue(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, issueKey))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetProjects() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.SUCCESS)
                        .projects(new ArrayList<>())
                        .build());

    List<JiraProjectDTO> projects = jiraResourceService.getProjects(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(projects).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetProjectsDelegateError() {
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("Error").build());

    assertThatThrownBy(() -> jiraResourceService.getProjects(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetProjectStatuses() {
    final String projectKey = "CDNG";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.SUCCESS)
                        .statuses(new ArrayList<>())
                        .build());

    List<JiraIssueTypeDTO> projectStatuses =
        jiraResourceService.getProjectStatuses(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, projectKey);

    assertThat(projectStatuses).isNotNull();
    assertThat(projectStatuses).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetProjectStatusesFailure() {
    final String projectKey = "CDNG";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("Error Message")
                        .build());

    assertThatThrownBy(
        () -> jiraResourceService.getProjectStatuses(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, projectKey))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetFieldsOptions() {
    final String projectKey = "CDNG";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.SUCCESS)
                        .fields(new ArrayList<>())
                        .build());

    List<JiraFieldDTO> fieldsOptions =
        jiraResourceService.getFieldsOptions(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, projectKey);

    assertThat(fieldsOptions).isNotNull();
    assertThat(fieldsOptions).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetFieldsOptionsFailure() {
    final String projectKey = "CDNG";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("Error Message")
                        .build());

    assertThatThrownBy(
        () -> jiraResourceService.getFieldsOptions(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, projectKey))
        .isInstanceOf(HarnessJiraException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCheckApproval() {
    final String issueId = "CDNG-1345";
    final String approvalField = "status";
    final String approvalFieldValue = "Success";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.SUCCESS)
                        .currentStatus(approvalFieldValue)
                        .build());

    JiraApprovalDTO jiraApprovalDTO = jiraResourceService.checkApproval(
        identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, issueId, approvalField, approvalFieldValue, null, null);

    assertThat(jiraApprovalDTO).isNotNull();
    assertThat(jiraApprovalDTO.isApproved()).isTrue();
    assertThat(jiraApprovalDTO.getStatus()).isEqualTo(approvalFieldValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetCreateMetadata() {
    final String projectKey = "CDNG";
    final String expand = "expand";
    IdentifierRef identifierRef = createIdentifier();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(JiraTaskNGResponse.builder()
                        .executionStatus(CommandExecutionStatus.SUCCESS)
                        .createMetadata(new JiraCreateMetaResponse(
                            new JSONObject().element(expand, expand).element("projects", "[]")))
                        .build());

    JiraGetCreateMetadataDTO jiraGetCreateMetadataDTO =
        jiraResourceService.getCreateMetadata(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, projectKey, expand);

    assertThat(jiraGetCreateMetadataDTO).isNotNull();
    assertThat(jiraGetCreateMetadataDTO.getExpand()).isEqualTo(expand);
    assertThat(jiraGetCreateMetadataDTO.getProjects()).isEmpty();
  }

  private ConnectorResponseDTO getConnector() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(ConnectorType.JIRA)
                                            .connectorConfig(JiraConnectorDTO.builder()
                                                                 .jiraUrl("url")
                                                                 .username("username")
                                                                 .passwordRef(SecretRefData.builder().build())
                                                                 .build())
                                            .build();
    return ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
  }

  private IdentifierRef createIdentifier() {
    return IdentifierRef.builder()
        .accountIdentifier(ACCOUNT_ID)
        .identifier(IDENTIFIER)
        .projectIdentifier(PROJECT_IDENTIFIER)
        .orgIdentifier(ORG_IDENTIFIER)
        .build();
  }
}
