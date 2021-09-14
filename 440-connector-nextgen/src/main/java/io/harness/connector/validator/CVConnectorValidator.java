/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.validator;

import static software.wings.beans.TaskType.CVNG_CONNECTOR_VALIDATE_TASK;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class CVConnectorValidator extends AbstractConnectorValidator {
  @Inject private final SecretManagerClientService ngSecretService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private NGErrorHelper ngErrorHelper;

  private String getDelegateId(DelegateTaskNotifyResponseData cvConnectorTaskResponse) {
    if (cvConnectorTaskResponse == null || cvConnectorTaskResponse.getDelegateMetaInfo() == null) {
      return null;
    }
    return cvConnectorTaskResponse.getDelegateMetaInfo().getId();
  }

  private List<ErrorDetail> getErrorDetail(String errorMessage) {
    return Collections.singletonList(
        ErrorDetail.builder().message(errorMessage).code(450).reason("Invalid Credentials").build());
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final List<EncryptedDataDetail> encryptionDetail =
        super.getEncryptionDetail(connectorConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return CVConnectorTaskParams.builder()
        .connectorConfigDTO(connectorConfig)
        .encryptionDetails(encryptionDetail)
        .build();
  }

  @Override
  public String getTaskType() {
    return CVNG_CONNECTOR_VALIDATE_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final DelegateResponseData delegateResponseData =
        super.validateConnector(connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    CVConnectorTaskResponse cvConnectorTaskResponse = (CVConnectorTaskResponse) delegateResponseData;
    return ConnectorValidationResult.builder()
        .status(cvConnectorTaskResponse.isValid() ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE)
        .errorSummary(cvConnectorTaskResponse.getErrorMessage())
        .delegateId(getDelegateId(cvConnectorTaskResponse))
        .build();
  }
}
