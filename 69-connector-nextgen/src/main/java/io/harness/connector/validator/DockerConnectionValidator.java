package io.harness.connector.validator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class DockerConnectionValidator implements ConnectionValidator<DockerConnectorDTO> {
  private final DelegateGrpcClientWrapper delegateClient;
  private final SecretManagerClientService ngSecretService;

  @Override
  public ConnectorValidationResult validate(
      DockerConnectorDTO dockerConnector, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerAuthCredentialsDTO dockerAuthCredentials =
        dockerConnector.getAuth() != null ? dockerConnector.getAuth().getCredentials() : null;
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList = null;
    if (dockerAuthCredentials != null) {
      encryptedDataDetailList = ngSecretService.getEncryptionDetails(basicNGAccessObject, dockerAuthCredentials);
    }
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType("DOCKER_CONNECTIVITY_TEST_TASK")
                                                  .taskParameters(DockerTestConnectionTaskParams.builder()
                                                                      .dockerConnector(dockerConnector)
                                                                      .encryptionDetails(encryptedDataDetailList)
                                                                      .build())
                                                  .executionTimeout(Duration.ofMinutes(1))
                                                  .build();
    DockerTestConnectionTaskResponse responseData =
        (DockerTestConnectionTaskResponse) delegateClient.executeSyncTask(delegateTaskRequest);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }
}
