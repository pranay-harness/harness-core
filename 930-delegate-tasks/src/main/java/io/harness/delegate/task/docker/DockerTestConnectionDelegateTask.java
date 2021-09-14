/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.docker;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.beans.connector.docker.DockerValidationParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class DockerTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  private static final String EMPTY_STR = "";
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
    final DockerValidationParams dockerValidationParams =
        DockerValidationParams.builder()
            .encryptionDataDetails(dockerConnectionTaskResponse.getEncryptionDetails())
            .dockerConnectorDTO(dockerConnectorDTO)
            .build();
    ConnectorValidationResult dockerConnectorValidationResult =
        dockerValidationHandler.validate(dockerValidationParams, getAccountId());
    dockerConnectorValidationResult.setDelegateId(getDelegateId());
    return DockerTestConnectionTaskResponse.builder()
        .connectorValidationResult(dockerConnectorValidationResult)
        .build();
  }

  @Override
  public DockerTestConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
