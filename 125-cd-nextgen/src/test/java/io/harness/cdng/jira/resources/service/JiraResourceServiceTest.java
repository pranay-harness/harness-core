package io.harness.cdng.jira.resources.service;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.exception.HarnessJiraException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

public class JiraResourceServiceTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";

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
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();

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
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();

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
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();

    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.FAILURE).errorMessage("Error").build());

    assertThatThrownBy(() -> jiraResourceService.validateCredentials(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER))
        .isInstanceOf(HarnessJiraException.class);
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
}
