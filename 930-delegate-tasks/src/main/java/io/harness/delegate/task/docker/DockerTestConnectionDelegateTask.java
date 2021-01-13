package io.harness.delegate.task.docker;

import io.harness.artifacts.docker.service.DockerRegistryService;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactTaskHelper;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.k8s.ConnectorValidationHandler;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
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
  @Inject private DockerValidationHandler dockerValidationHandler;

  public DockerTestConnectionDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DockerTestConnectionTaskResponse run(TaskParameters parameters) {
    DockerTestConnectionTaskParams dockerConnectionTaskResponse = (DockerTestConnectionTaskParams) parameters;
    DockerConnectorDTO dockerConnectorDTO = dockerConnectionTaskResponse.getDockerConnector();
    ConnectorValidationResult dockerConnectorValidationResult = dockerValidationHandler.validate(
        dockerConnectorDTO, getAccountId(), ((DockerTestConnectionTaskParams) parameters).getEncryptionDetails());
    dockerConnectorValidationResult.setDelegateId(getDelegateId());
    return DockerTestConnectionTaskResponse.builder()
        .connectorValidationResult(dockerConnectorValidationResult)
        .build();
  }

  @Override
  public DockerTestConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  public static class DockerValidationHandler extends ConnectorValidationHandler {
    @Inject private DockerArtifactTaskHelper dockerArtifactTaskHelper;
    @Inject private NGErrorHelper ngErrorHelper;

    @Override
    public ConnectorValidationResult validate(
        ConnectorConfigDTO connector, String accountIdentifier, List<EncryptedDataDetail> encryptionDetailList) {
      ArtifactTaskParameters artifactTaskParameters =
          ArtifactTaskParameters.builder()
              .accountId(accountIdentifier)
              .artifactTaskType(ArtifactTaskType.VALIDATE_ARTIFACT_SERVER)
              .attributes(DockerArtifactDelegateRequest.builder()
                              .dockerConnectorDTO((DockerConnectorDTO) connector)
                              .encryptedDataDetails(encryptionDetailList)
                              .build())
              .build();
      ArtifactTaskResponse validationResponse =
          dockerArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
      boolean isDockerCredentialsValid = false;
      ConnectorValidationResultBuilder validationResultBuilder = ConnectorValidationResult.builder();
      if (validationResponse.getArtifactTaskExecutionResponse() != null) {
        isDockerCredentialsValid = validationResponse.getArtifactTaskExecutionResponse().isArtifactServerValid();
      }
      validationResultBuilder.status(
          isDockerCredentialsValid ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE);
      if (!isDockerCredentialsValid) {
        String errorMessage = validationResponse.getErrorMessage();
        validationResultBuilder.errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
            .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)));
      }
      return validationResultBuilder.build();
    }
  }
}
