package io.harness.delegate.task.nexus;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusValidationParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.nexus.NexusTaskParams;
import io.harness.delegate.beans.nexus.NexusTaskParams.TaskType;
import io.harness.delegate.beans.nexus.NexusTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDetail;
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
public class NexusDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService decryptionService;
  @Inject NexusMapper nexusMapper;
  @Inject NGErrorHelper ngErrorHelper;
  @Inject NexusValidationHandler nexusValidationHandler;

  public NexusDelegateTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    NexusTaskParams taskParams = (NexusTaskParams) parameters;
    final NexusConnectorDTO nexusConfig = taskParams.getNexusConnectorDTO();
    final List<EncryptedDataDetail> encryptionDetails = taskParams.getEncryptedDataDetails();
    decryptionService.decrypt(nexusConfig.getAuth().getCredentials(), encryptionDetails);
    final TaskType taskType = taskParams.getTaskType();
    try {
      switch (taskType) {
        case VALIDATE:
          return validateNexusServer(nexusConfig, encryptionDetails);
        default:
          throw new InvalidRequestException("No task found for " + taskType.name());
      }
    } catch (Exception e) {
      String errorMessage = e.getMessage();
      String errorSummary = ngErrorHelper.getErrorSummary(errorMessage);
      ErrorDetail errorDetail = ngErrorHelper.createErrorDetail(errorMessage);
      ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                                .testedAt(System.currentTimeMillis())
                                                                .delegateId(getDelegateId())
                                                                .status(ConnectivityStatus.FAILURE)
                                                                .errorSummary(errorSummary)
                                                                .errors(Collections.singletonList(errorDetail))
                                                                .build();
      return NexusTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
    }
  }

  private NexusTaskResponse validateNexusServer(
      NexusConnectorDTO nexusRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    final NexusValidationParams nexusValidationParams = NexusValidationParams.builder()
                                                            .encryptedDataDetails(encryptedDataDetails)
                                                            .nexusConnectorDTO(nexusRequest)
                                                            .build();
    ConnectorValidationResult connectorValidationResult =
        nexusValidationHandler.validate(nexusValidationParams, getAccountId());
    connectorValidationResult.setDelegateId(getDelegateId());
    return NexusTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }
}