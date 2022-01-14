package io.harness.connector.mappers.customhealthconnectormapper;

import io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnector;
import io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnectorKeyAndValue;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import java.util.stream.Collectors;

public class CustomHealthDTOToEntity
    implements ConnectorDTOToEntityMapper<CustomHealthConnectorDTO, CustomHealthConnector> {
  @Override
  public CustomHealthConnector toConnectorEntity(CustomHealthConnectorDTO connectorDTO) {
    return CustomHealthConnector.builder()
        .baseURL(connectorDTO.getBaseURL())
        .method(connectorDTO.getMethod())
        .validationBody(connectorDTO.getValidationBody())
        .validationPath(connectorDTO.getValidationPath())
        .headers(connectorDTO.getHeaders()
                     .stream()
                     .map(header
                         -> CustomHealthConnectorKeyAndValue.builder()
                                .key(header.getKey())
                                .value(header.getValue())
                                .isValueEncrypted(header.isValueEncrypted())
                                .encryptedValueRef(SecretRefHelper.getSecretConfigString(header.getEncryptedValueRef()))
                                .build())
                     .collect(Collectors.toList()))
        .params(connectorDTO.getParams()
                    .stream()
                    .map(params
                        -> CustomHealthConnectorKeyAndValue.builder()
                               .key(params.getKey())
                               .value(params.getValue())
                               .isValueEncrypted(params.isValueEncrypted())
                               .encryptedValueRef(SecretRefHelper.getSecretConfigString(params.getEncryptedValueRef()))
                               .build())
                    .collect(Collectors.toList()))

        .build();
  }
}
