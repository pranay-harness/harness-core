package io.harness.delegate.task.docker;

import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.service.DockerRegistryService;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class DockerTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  private static final String EMPTY_STR = "";
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private DockerConnectorToDockerInternalConfigMapper dockerConnectorToDockerInternalConfigMapper;
  @Inject private DockerRegistryService dockerRegistryService;

  public DockerTestConnectionDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DockerTestConnectionTaskResponse run(TaskParameters parameters) {
    DockerTestConnectionTaskParams dockerConnectionTaskResponse = (DockerTestConnectionTaskParams) parameters;
    DockerConnectorDTO dockerConnectorDTO = dockerConnectionTaskResponse.getDockerConnector();
    DockerAuthCredentialsDTO dockerAuthCredentialsDTO =
        dockerConnectorDTO.getAuth() != null ? dockerConnectorDTO.getAuth().getCredentials() : null;
    if (dockerAuthCredentialsDTO != null) {
      secretDecryptionService.decrypt(dockerAuthCredentialsDTO, dockerConnectionTaskResponse.getEncryptionDetails());
    }

    DockerInternalConfig dockerInternalConfig =
        dockerConnectorToDockerInternalConfigMapper.toDockerInternalConfig(dockerConnectorDTO);
    boolean validationResult;
    String errorMessage = EMPTY_STR;
    try {
      validationResult = dockerRegistryService.validateCredentials(dockerInternalConfig);
    } catch (Exception ex) {
      validationResult = false;
      errorMessage = ex.getMessage();
    }
    return DockerTestConnectionTaskResponse.builder()
        .connectionSuccessFul(validationResult)
        .errorMessage(errorMessage)
        .build();
  }

  @Override
  public DockerTestConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }
}
