package io.harness.connector;

import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.task.k8s.ConnectorValidationHandler;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
//@Builder -- do not add this
public class ConnectorRegistrar {
  ConnectorCategory connectorCategory;
  Class<? extends AbstractConnectorValidator> connectorValidator;
  Class<? extends ConnectorValidationHandler> connectorValidationHandler;
  Class<? extends ConnectorDTOToEntityMapper<?, ?>> connectorDTOToEntityMapper;
  Class<? extends ConnectorEntityToDTOMapper<?, ?>> connectorEntityToDTOMapper;
}
