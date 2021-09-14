/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.utils;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.entities.ConnectorFilterProperties;
import io.harness.delegate.beans.connector.ConnectorType;

import java.util.Arrays;
import java.util.Collections;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorFilterTestHelper {
  public ConnectorFilterPropertiesDTO createConnectorFilterPropertiesDTOForTest() {
    return ConnectorFilterPropertiesDTO.builder()
        .categories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER))
        .connectorNames(Arrays.asList("Connector 1", "Connector 2"))
        .connectivityStatuses(Collections.singletonList(ConnectivityStatus.SUCCESS))
        .connectorIdentifiers(Arrays.asList("Connector identifier 1", "Connector identifier 2"))
        .description("Connector description 1")
        .inheritingCredentialsFromDelegate(true)
        .types(Collections.singletonList(ConnectorType.KUBERNETES_CLUSTER))
        .build();
  }

  public ConnectorFilterProperties createConnectorFilterPropertiesEntityForTest() {
    return ConnectorFilterProperties.builder()
        .categories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER))
        .connectorNames(Arrays.asList("Connector 1", "Connector 2"))
        .connectivityStatuses(Collections.singletonList(ConnectivityStatus.SUCCESS))
        .connectorIdentifiers(Arrays.asList("Connector identifier 1", "Connector identifier 2"))
        .description("Connector description 1")
        .inheritingCredentialsFromDelegate(true)
        .types(Collections.singletonList(ConnectorType.KUBERNETES_CLUSTER))
        .build();
  }
}
