package io.harness.connector.mappers.prometheusmapper;

import io.harness.connector.entities.embedded.prometheusconnector.PrometheusConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;

public class PrometheusDTOToEntity implements ConnectorDTOToEntityMapper<PrometheusConnectorDTO, PrometheusConnector> {
  @Override
  public PrometheusConnector toConnectorEntity(PrometheusConnectorDTO connectorDTO) {
    return PrometheusConnector.builder().url(connectorDTO.getUrl()).build();
  }
}
