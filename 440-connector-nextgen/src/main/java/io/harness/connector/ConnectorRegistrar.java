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
  /**
   * Category of the connector.
   */
  ConnectorCategory connectorCategory;
  /**
   * The connector validator which will be executed during connector creation to check if connector is able to connect
   * or not.
   */
  Class<? extends ConnectionValidator> connectorValidator;
  /**
   * The connector Validation Params provider helps in perpetual task based validation of connectors every 10 minutes on
   * delegate agent to see if connector is able to connect.
   * {@link ConnectorValidationHandler} needs to be implemented to execute heartbeat on delegate.
   */
  Class<? extends ConnectorValidationParamsProvider> connectorValidationParams;
  /**
   * Rest model to db model mapper.
   */
  Class<? extends ConnectorDTOToEntityMapper<?, ?>> connectorDTOToEntityMapper;
  /**
   * Db model to rest model mapper.
   */
  Class<? extends ConnectorEntityToDTOMapper<?, ?>> connectorEntityToDTOMapper;


  Class<? extends ConnectorValidationHandler> connectorValidationHandler;

}
