/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
