package io.harness.connector.mappers.kubernetesMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dtos.K8Connector.KubernetesConfigSummaryDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesConfigSummaryDTO.KubernetesConfigSummaryDTOBuilder;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterConfig;

@Singleton
public class KubernetesConfigSummaryMapper {
  @Inject private KubernetesConfigCastHelper kubernetesConfigCastHelper;
  public KubernetesConfigSummaryDTO createKubernetesConfigSummaryDTO(KubernetesClusterConfig connector) {
    KubernetesConfigSummaryDTOBuilder builder = KubernetesConfigSummaryDTO.builder();
    populateURLORDelegateName(builder, connector);
    return builder.build();
  }

  private void populateURLORDelegateName(KubernetesConfigSummaryDTOBuilder builder, KubernetesClusterConfig connector) {
    if (connector.getCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      builder.delegateName(
          kubernetesConfigCastHelper.castToKubernetesDelegateCredential(connector.getCredential()).getDelegateName());
    } else if (connector.getCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      builder.masterURL(
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(connector.getCredential()).getMasterUrl());
    } else {
      throw new UnsupportedOperationException(
          String.format("The kubernetes config type %s is invalid", connector.getCredentialType()));
    }
  }
}
