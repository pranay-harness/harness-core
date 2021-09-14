/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectorEntityToDTOMapper<D extends ConnectorConfigDTO, B extends Connector> {
  D createConnectorDTO(B connector);
}
