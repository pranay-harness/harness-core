/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.validator;

import static software.wings.beans.TaskType.DOCKER_CONNECTIVITY_TEST_TASK;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskParams;
import io.harness.delegate.beans.connector.docker.DockerTestConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DockerConnectionValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerConnectorDTO dockerConnector = (DockerConnectorDTO) connectorConfig;
    DockerAuthCredentialsDTO dockerAuthCredentials =
        dockerConnector.getAuth() != null ? dockerConnector.getAuth().getCredentials() : null;
    return DockerTestConnectionTaskParams.builder()
        .dockerConnector(dockerConnector)
        .encryptionDetails(
            super.getEncryptionDetail(dockerAuthCredentials, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return DOCKER_CONNECTIVITY_TEST_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO dockerConnector, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    DockerTestConnectionTaskResponse responseData = (DockerTestConnectionTaskResponse) super.validateConnector(
        dockerConnector, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return responseData.getConnectorValidationResult();
  }
}
