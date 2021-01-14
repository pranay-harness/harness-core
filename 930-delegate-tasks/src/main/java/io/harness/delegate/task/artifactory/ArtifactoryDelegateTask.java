package io.harness.delegate.task.artifactory;

import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryServiceImpl;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskParams;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskParams.TaskType;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskResponse;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthCredentialsDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

public class ArtifactoryDelegateTask extends AbstractDelegateRunnableTask {
  @Inject SecretDecryptionService decryptionService;
  @Inject ArtifactoryRequestMapper artifactoryRequestMapper;
  @Inject ArtifactoryServiceImpl artifactoryService;
  public ArtifactoryDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    final ArtifactoryTaskParams artifactoryTaskParams = (ArtifactoryTaskParams) parameters;
    final ArtifactoryConnectorDTO artifactoryConnectorDTO = artifactoryTaskParams.getArtifactoryConnectorDTO();
    final List<EncryptedDataDetail> encryptedDataDetails = artifactoryTaskParams.getEncryptedDataDetails();
    final ArtifactoryAuthCredentialsDTO credentials = artifactoryConnectorDTO.getAuth().getCredentials();
    decryptionService.decrypt(credentials, encryptedDataDetails);
    final TaskType taskType = artifactoryTaskParams.getTaskType();
    final ArtifactoryConfigRequest artifactoryConfigRequest =
        artifactoryRequestMapper.toArtifactoryRequest(artifactoryConnectorDTO);
    try {
      switch (taskType) {
        case VALIDATE:
          return validateArtifactoryConfig(artifactoryConfigRequest);
        default:
          throw new InvalidRequestException("No task found for " + taskType.name());
      }
    } catch (Exception e) {
      final ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                                      .testedAt(System.currentTimeMillis())
                                                                      .delegateId(getDelegateId())
                                                                      .status(ConnectivityStatus.FAILURE)
                                                                      .errorSummary(e.getMessage())
                                                                      .build();
      return ArtifactoryTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
    }
  }

  private DelegateResponseData validateArtifactoryConfig(ArtifactoryConfigRequest artifactoryConfigRequest) {
    ConnectorValidationResult connectorValidationResult;
    boolean running = artifactoryService.validateArtifactServer(artifactoryConfigRequest);
    if (running) {
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.SUCCESS)
                                      .testedAt(System.currentTimeMillis())
                                      .delegateId(getDelegateId())
                                      .build();
    } else {
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.FAILURE)
                                      .testedAt(System.currentTimeMillis())
                                      .delegateId(getDelegateId())
                                      .build();
    }
    return ArtifactoryTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }
}
