package io.harness.delegate.task.nexus;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.nexus.NexusTaskParams;
import io.harness.delegate.beans.nexus.NexusTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.WingsException;
import io.harness.nexus.NexusClientImpl;
import io.harness.nexus.NexusRequest;
import io.harness.nexus.NexusThreeClientImpl;
import io.harness.nexus.NexusTwoClientImpl;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class NexusDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private TimeLimiter timeLimiter;
  @Inject NexusClientImpl nexusClient;
  @Inject NexusMapper nexusMapper;

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
    final NexusRequest nexusRequest = nexusMapper.toNexusRequest(nexusConfig);
    ConnectorValidationResult connectorValidationResult;
    try {
      boolean running = nexusClient.isRunning(nexusRequest);
      if (!running) {
        connectorValidationResult = ConnectorValidationResult.builder()
                                        .status(ConnectivityStatus.SUCCESS)
                                        .testedAt(System.currentTimeMillis())
                                        .delegateId(getDelegateId())
                                        .build();
      } else {
        connectorValidationResult = ConnectorValidationResult.builder()
                                        .status(ConnectivityStatus.SUCCESS)
                                        .testedAt(System.currentTimeMillis())
                                        .delegateId(getDelegateId())
                                        .build();
      }
    } catch (Exception e) {
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .testedAt(System.currentTimeMillis())
                                      .delegateId(getDelegateId())
                                      .status(ConnectivityStatus.FAILURE)
                                      .errorSummary(e.getMessage())
                                      .build();
    }
    return NexusTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }
}