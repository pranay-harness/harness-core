/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.validator;

import static software.wings.beans.TaskType.HTTP_HELM_CONNECTIVITY_TASK;

import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthCredentialsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectivityTaskParams;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectivityTaskResponse;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class HttpHelmRepoConnectionValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    HttpHelmConnectorDTO helmConnector = (HttpHelmConnectorDTO) connectorConfig;
    HttpHelmAuthCredentialsDTO helmAuthCredentials =
        helmConnector.getAuth() != null ? helmConnector.getAuth().getCredentials() : null;
    return HttpHelmConnectivityTaskParams.builder()
        .helmConnector(helmConnector)
        .encryptionDetails(
            super.getEncryptionDetail(helmAuthCredentials, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return HTTP_HELM_CONNECTIVITY_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    HttpHelmConnectivityTaskResponse responseData = (HttpHelmConnectivityTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return responseData.getConnectorValidationResult();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }
}
