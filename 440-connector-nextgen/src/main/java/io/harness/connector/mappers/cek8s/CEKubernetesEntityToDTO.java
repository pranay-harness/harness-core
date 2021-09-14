/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.cek8s;

import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;

import com.google.inject.Singleton;

@Singleton
public class CEKubernetesEntityToDTO implements ConnectorEntityToDTOMapper<CEKubernetesClusterConfigDTO, CEK8sDetails> {
  @Override
  public CEKubernetesClusterConfigDTO createConnectorDTO(CEK8sDetails connector) {
    return CEKubernetesClusterConfigDTO.builder()
        .featuresEnabled(connector.getFeaturesEnabled())
        .connectorRef(connector.getConnectorRef())
        .build();
  }
}
