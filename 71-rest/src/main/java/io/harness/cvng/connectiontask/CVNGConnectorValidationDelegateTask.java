package io.harness.cvng.connectiontask;

import com.google.inject.Inject;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.ConnectorValidationInfo;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
@Slf4j
public class CVNGConnectorValidationDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private Clock clock;
  public CVNGConnectorValidationDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new IllegalStateException("run should not get called with Object parameters");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CVConnectorTaskParams taskParameters = (CVConnectorTaskParams) parameters;
    if (taskParameters.getConnectorConfigDTO() instanceof DecryptableEntity) {
      secretDecryptionService.decrypt(
          (DecryptableEntity) taskParameters.getConnectorConfigDTO(), taskParameters.getEncryptionDetails());
    }
    boolean validCredentials = false;
    Exception exceptionInProcessing = null;
    try {
      ConnectorValidationInfo connectorValidationInfo =
          ConnectorValidationInfo.getConnectorValidationInfo(taskParameters.getConnectorConfigDTO());
      String dsl = connectorValidationInfo.getConnectionValidationDSL();
      Instant now = clock.instant();
      final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                      .baseUrl(connectorValidationInfo.getBaseUrl())
                                                      .commonHeaders(connectorValidationInfo.collectionHeaders())
                                                      .commonOptions(connectorValidationInfo.collectionParams())
                                                      .otherEnvVariables(connectorValidationInfo.getDslEnvVariables())
                                                      .endTime(connectorValidationInfo.getEndTime(now))
                                                      .startTime(connectorValidationInfo.getStartTime(now))
                                                      .build();
      validCredentials = ((String) dataCollectionDSLService.execute(dsl, runtimeParameters)).equalsIgnoreCase("true");
      log.info("connectorValidationInfo {}", connectorValidationInfo);
    } catch (Exception ex) {
      log.info("Exception while validating connector credentials", ex);
      exceptionInProcessing = ex;
    }

    return CVConnectorTaskResponse.builder()
        .valid(validCredentials)
        .errorMessage(exceptionInProcessing != null ? exceptionInProcessing.getMessage() : null)
        .build();
  }
}
