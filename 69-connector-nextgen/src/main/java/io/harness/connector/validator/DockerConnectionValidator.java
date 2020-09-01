package io.harness.connector.validator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class DockerConnectionValidator implements ConnectionValidator<DockerConnectorDTO> {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private final SecretManagerClientService ngSecretService;

  @Override
  public ConnectorValidationResult validate(
      DockerConnectorDTO dockerConnector, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerAuthCredentialsDTO dockerAuthCredentials =
        dockerConnector.getAuthScheme() != null ? dockerConnector.getAuthScheme().getCredentials() : null;
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList = null;
    if (dockerAuthCredentials != null) {
      encryptedDataDetailList = ngSecretService.getEncryptionDetails(basicNGAccessObject, dockerAuthCredentials);
    }
    TaskData taskData = TaskData.builder()
                            .async(false)
                            .taskType("DOCKER_CONNECTIVITY_TEST_TASK")
                            .parameters(new Object[] {DockerTestConnectionTaskParams.builder()
                                                          .dockerConnector(dockerConnector)
                                                          .encryptionDetails(encryptedDataDetailList)
                                                          .build()})
                            .timeout(TimeUnit.MINUTES.toMillis(1))
                            .build();
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountIdentifier);
    DockerTestConnectionTaskResponse responseData =
        managerDelegateServiceDriver.sendTask(accountIdentifier, setupAbstractions, taskData);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }
}
