package io.harness.connector.mappers.prometheusmapper;

import io.harness.connector.entities.embedded.prometheusconnector.PrometheusConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;

public class PrometheusEntityToDTO implements ConnectorEntityToDTOMapper<PrometheusConnectorDTO, PrometheusConnector> {
  @Override
  public PrometheusConnectorDTO createConnectorDTO(PrometheusConnector connector) {
    return PrometheusConnectorDTO.builder().url(connector.getUrl()).build();
  }
}
