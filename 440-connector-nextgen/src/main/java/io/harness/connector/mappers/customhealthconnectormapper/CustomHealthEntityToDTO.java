/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.customhealthconnectormapper;

import io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthConnectorDTO;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthKeyAndValue;
import io.harness.encryption.SecretRefHelper;

import java.util.stream.Collectors;

public class CustomHealthEntityToDTO
    implements ConnectorEntityToDTOMapper<CustomHealthConnectorDTO, CustomHealthConnector> {
  @Override
  public CustomHealthConnectorDTO createConnectorDTO(CustomHealthConnector connector) {
    return CustomHealthConnectorDTO.builder()
        .baseURL(connector.getBaseURL())
        .method(connector.getMethod())
        .validationBody(connector.getValidationBody())
        .validationPath(connector.getValidationPath())
        .headers(connector.getHeaders()
                     .stream()
                     .map(header -> {
                       return CustomHealthKeyAndValue.builder()
                           .key(header.getKey())
                           .value(header.getValue())
                           .isValueEncrypted(header.isValueEncrypted())
                           .encryptedValueRef(SecretRefHelper.createSecretRef(header.getEncryptedValueRef()))
                           .build();
                     })
                     .collect(Collectors.toList()))
        .params(connector.getParams()
                    .stream()
                    .map(param -> {
                      return CustomHealthKeyAndValue.builder()
                          .key(param.getKey())
                          .value(param.getValue())
                          .isValueEncrypted(param.isValueEncrypted())
                          .encryptedValueRef(SecretRefHelper.createSecretRef(param.getEncryptedValueRef()))
                          .build();
                    })
                    .collect(Collectors.toList()))
        .build();
  }
}
