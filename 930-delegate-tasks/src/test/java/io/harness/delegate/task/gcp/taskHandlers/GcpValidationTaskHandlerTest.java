package io.harness.delegate.task.gcp.taskHandlers;

import static io.harness.delegate.beans.connector.ConnectivityStatus.FAILURE;
import static io.harness.delegate.beans.connector.ConnectivityStatus.SUCCESS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.gcp.client.GcpClient;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GcpValidationTaskHandlerTest extends CategoryTest {
  @Mock private GcpClient gcpClient;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private NGErrorHelper ngErrorHelper;
  @InjectMocks private GcpValidationTaskHandler taskHandler;

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

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeRequestFailure() {
    doThrow(new RuntimeException("No Default Credentials found")).when(gcpClient).validateDefaultCredentials();
    final GcpValidationTaskResponse response =
        (GcpValidationTaskResponse) taskHandler.executeRequest(buildGcpValidationRequest());
    ConnectorValidationResult connectorValidationResult = response.getConnectorValidationResult();
    assertThat(connectorValidationResult.getStatus()).isEqualTo(FAILURE);
  }

  private GcpValidationRequest buildGcpValidationRequest() {
    return GcpValidationRequest.builder().delegateSelector("foo").build();
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

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void executeRequestFailureForSecretKey() {
    doThrow(new RuntimeException("No Credentials found")).when(gcpClient).getGkeContainerService(any());
    final GcpValidationTaskResponse gcpResponse =
        (GcpValidationTaskResponse) taskHandler.executeRequest(buildGcpValidationRequestWithSecretKey());
    ConnectorValidationResult connectorValidationResult = gcpResponse.getConnectorValidationResult();
    assertThat(connectorValidationResult.getStatus()).isEqualTo(FAILURE);
  }

  private GcpValidationRequest buildGcpValidationRequestWithSecretKey() {
    return GcpValidationRequest.builder()
        .gcpManualDetailsDTO(GcpManualDetailsDTO.builder().secretKeyRef(SecretRefData.builder().build()).build())
        .build();
  }
}
