/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.validator;

import static io.harness.delegate.beans.artifactory.ArtifactoryTaskParams.TaskType.VALIDATE;

import static software.wings.beans.TaskType.NG_ARTIFACTORY_TASK;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskParams;
import io.harness.delegate.beans.artifactory.ArtifactoryTaskResponse;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ArtifactoryConnectionValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ArtifactoryConnectorDTO connectorDTO = (ArtifactoryConnectorDTO) connectorConfig;

    return ArtifactoryTaskParams.builder()
        .taskType(VALIDATE)
        .artifactoryConnectorDTO(connectorDTO)
        .encryptedDataDetails(super.getEncryptionDetail(
            connectorDTO.getAuth().getCredentials(), accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return NG_ARTIFACTORY_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    ArtifactoryTaskResponse responseData = (ArtifactoryTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return responseData.getConnectorValidationResult();
  }
}
