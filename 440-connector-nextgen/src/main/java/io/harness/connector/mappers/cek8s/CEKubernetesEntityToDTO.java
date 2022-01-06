/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
