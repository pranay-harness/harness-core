package io.harness.connector.mappers.cek8s;

import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;

import com.google.inject.Singleton;

@Singleton
public class CEKubernetesDTOToEntity implements ConnectorDTOToEntityMapper<CEKubernetesClusterConfigDTO, CEK8sDetails> {
  @Override
  public CEK8sDetails toConnectorEntity(CEKubernetesClusterConfigDTO connectorDTO) {
    return CEK8sDetails.builder().connectorRef(connectorDTO.getConnectorRef()).build();
  }
}
