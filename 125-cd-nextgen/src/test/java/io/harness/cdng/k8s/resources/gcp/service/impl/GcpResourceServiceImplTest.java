package io.harness.cdng.k8s.resources.gcp.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.k8s.resources.gcp.GcpResponseDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.GcpTaskType;
import io.harness.delegate.task.gcp.request.GcpTaskParameters;
import io.harness.delegate.task.gcp.response.GcpClusterListTaskResponse;
import io.harness.encryption.SecretRefData;
import io.harness.exception.GcpServerException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class GcpResourceServiceImplTest extends CategoryTest {
  private static String ACCOUNT_ID = "accountId";
  private static String ORG_IDENTIFIER = "orgIdentifier";
  private static String PROJECT_IDENTIFIER = "projectIdentifier";

  @Mock ConnectorService connectorService;
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @InjectMocks GcpResourceServiceImpl gcpResourceService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetClusterNamesSuccess() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(Arrays.asList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(GcpClusterListTaskResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .clusterNames(Arrays.asList("cluster1", "cluster2"))
                        .build());

    GcpResponseDTO responseDTO =
        gcpResourceService.getClusterNames(identifierRef, ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.getClusterNames()).containsExactly("cluster1", "cluster2");

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService, times(1)).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    GcpTaskParameters gcpTaskParameters = (GcpTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(gcpTaskParameters.getGcpTaskType()).isEqualTo(GcpTaskType.LIST_CLUSTERS);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetClusterNamesErrorNotifyResponse() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(Arrays.asList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("Bad credentials").build());

    try {
      gcpResourceService.getClusterNames(identifierRef, ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
      fail("Should throw gcp exception instead");
    } catch (GcpServerException gcpServerException) {
      assertThat(gcpServerException.getMessage())
          .isEqualTo("GCP cluster list task failure due to error - Bad credentials");
    }

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService, times(1)).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    GcpTaskParameters gcpTaskParameters = (GcpTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(gcpTaskParameters.getGcpTaskType()).isEqualTo(GcpTaskType.LIST_CLUSTERS);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetClusterNamesFailure() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(Arrays.asList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(GcpClusterListTaskResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("Bad credentials")
                        .errorDetail(ErrorDetail.builder().message("Invalid credentials").build())
                        .build());

    try {
      gcpResourceService.getClusterNames(identifierRef, ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
      fail("Should throw gcp exception instead");
    } catch (GcpServerException gcpServerException) {
      assertThat(gcpServerException.getMessage())
          .isEqualTo(
              "GCP cluster list task failure due to error - Bad credentials with error detail: ErrorDetail(reason=null, message=Invalid credentials, code=0)");
    }

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService, times(1)).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    GcpTaskParameters gcpTaskParameters = (GcpTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(gcpTaskParameters.getGcpTaskType()).isEqualTo(GcpTaskType.LIST_CLUSTERS);
  }

  private ConnectorResponseDTO getConnector() {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorType(ConnectorType.GCP)
            .connectorConfig(
                GcpConnectorDTO.builder()
                    .credential(
                        GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                            .config(GcpManualDetailsDTO.builder()
                                        .secretKeyRef(
                                            SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                        .build())
                            .build())
                    .build())
            .build();
    return ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
  }
}
