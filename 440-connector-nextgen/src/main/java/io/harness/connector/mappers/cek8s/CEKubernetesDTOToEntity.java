/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.cek8s;

import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class CEKubernetesDTOToEntity implements ConnectorDTOToEntityMapper<CEKubernetesClusterConfigDTO, CEK8sDetails> {
  @Override
  public CEK8sDetails toConnectorEntity(CEKubernetesClusterConfigDTO connectorDTO) {
    final List<CEFeatures> featuresEnabled = connectorDTO.getFeaturesEnabled();
    return CEK8sDetails.builder().featuresEnabled(featuresEnabled).connectorRef(connectorDTO.getConnectorRef()).build();
  }
}
