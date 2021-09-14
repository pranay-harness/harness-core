/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.connectiontask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DecryptableEntity;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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
  public boolean isSupportingErrorFramework() {
    return true;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CVConnectorTaskParams taskParameters = (CVConnectorTaskParams) parameters;
    if (taskParameters.getConnectorConfigDTO() instanceof DecryptableEntity) {
      secretDecryptionService.decrypt(
          (DecryptableEntity) taskParameters.getConnectorConfigDTO(), taskParameters.getEncryptionDetails());
    }
    boolean validCredentials = false;

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

    return CVConnectorTaskResponse.builder()
        .valid(validCredentials)
        .delegateMetaInfo(DelegateMetaInfo.builder().id(getDelegateId()).build())
        .build();
  }
}
