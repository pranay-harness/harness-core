/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.gcp.taskHandlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.connector.ConnectivityStatus.SUCCESS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcp.GcpValidationParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.GcpRequestMapper;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.gcp.client.GcpClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class GcpValidationTaskHandlerTest extends CategoryTest {
  @Mock private GcpClient gcpClient;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private GcpRequestMapper gcpRequestMapper;
  @InjectMocks private GcpValidationTaskHandler taskHandler;
  private String accountIdentifier = "accountIdentifier";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeRequestSuccess() {
    final GcpValidationTaskResponse response =
        (GcpValidationTaskResponse) taskHandler.executeRequest(buildGcpValidationRequest());
    ConnectorValidationResult connectorValidationResult = response.getConnectorValidationResult();
    assertThat(connectorValidationResult.getStatus()).isEqualTo(SUCCESS);
  }

  @Test(expected = RuntimeException.class)
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeRequestFailure() {
    doThrow(new RuntimeException("No Default Credentials found")).when(gcpClient).validateDefaultCredentials();
    taskHandler.executeRequest(buildGcpValidationRequest());
  }

  private GcpValidationRequest buildGcpValidationRequest() {
    return GcpValidationRequest.builder().delegateSelectors(Collections.singleton("foo")).build();
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void executeRequestSuccessForSecretKey() {
    final GcpValidationTaskResponse response =
        (GcpValidationTaskResponse) taskHandler.executeRequest(buildGcpValidationRequestWithSecretKey());
    ConnectorValidationResult connectorValidationResult = response.getConnectorValidationResult();
    assertThat(connectorValidationResult.getStatus()).isEqualTo(SUCCESS);
  }

  @Test(expected = RuntimeException.class)
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void executeRequestFailureForSecretKey() {
    doThrow(new RuntimeException("No Credentials found")).when(gcpClient).getGkeContainerService(any());
    taskHandler.executeRequest(buildGcpValidationRequestWithSecretKey());
  }

  private GcpValidationRequest buildGcpValidationRequestWithSecretKey() {
    return GcpValidationRequest.builder()
        .gcpManualDetailsDTO(GcpManualDetailsDTO.builder().secretKeyRef(SecretRefData.builder().build()).build())
        .build();
  }

  @Test
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void validateSuccess() {
    doCallRealMethod().when(gcpRequestMapper).toGcpRequest(any());
    GcpManualDetailsDTO config = GcpManualDetailsDTO.builder().secretKeyRef(SecretRefData.builder().build()).build();
    GcpConnectorCredentialDTO gcpConnectorCredentialDTO = GcpConnectorCredentialDTO.builder()
                                                              .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                                                              .config(config)
                                                              .build();
    GcpConnectorDTO gcpConnectorDTO = GcpConnectorDTO.builder().credential(gcpConnectorCredentialDTO).build();
    ConnectorValidationParams connectorValidationParams = GcpValidationParams.builder()
                                                              .gcpConnectorDTO(gcpConnectorDTO)
                                                              .connectorName("GcpConnectorName")
                                                              .delegateSelectors(Collections.singleton("foo"))
                                                              .encryptionDetails(null)
                                                              .build();

    ConnectorValidationResult result = taskHandler.validate(connectorValidationParams, accountIdentifier);
    assertThat(result.getStatus()).isEqualTo(SUCCESS);
  }

  @Test(expected = RuntimeException.class)
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void validateFailure() {
    doThrow(new RuntimeException("No Credentials found")).when(gcpClient).getGkeContainerService(any());
    doCallRealMethod().when(gcpRequestMapper).toGcpRequest(any());
    GcpManualDetailsDTO config = GcpManualDetailsDTO.builder().secretKeyRef(SecretRefData.builder().build()).build();
    GcpConnectorCredentialDTO gcpConnectorCredentialDTO = GcpConnectorCredentialDTO.builder()
                                                              .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                                                              .config(config)
                                                              .build();
    GcpConnectorDTO gcpConnectorDTO = GcpConnectorDTO.builder().credential(gcpConnectorCredentialDTO).build();
    ConnectorValidationParams connectorValidationParams = GcpValidationParams.builder()
                                                              .gcpConnectorDTO(gcpConnectorDTO)
                                                              .connectorName("GcpConnectorName")
                                                              .encryptionDetails(null)
                                                              .build();

    taskHandler.validate(connectorValidationParams, accountIdentifier);
  }
}
