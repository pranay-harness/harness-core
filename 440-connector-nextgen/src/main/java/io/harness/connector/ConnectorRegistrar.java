package io.harness.connector;

import io.harness.connector.heartbeat.ConnectorValidationParamsProvider;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.validator.ConnectionValidator;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
//@Builder -- do not add this
public class ConnectorRegistrar {
  ConnectorCategory connectorCategory;
  Class<? extends ConnectionValidator> connectorValidator;
  Class<? extends ConnectorValidationParamsProvider> connectorValidationParams;
  Class<? extends ConnectorDTOToEntityMapper<?, ?>> connectorDTOToEntityMapper;
  Class<? extends ConnectorEntityToDTOMapper<?, ?>> connectorEntityToDTOMapper;
}
