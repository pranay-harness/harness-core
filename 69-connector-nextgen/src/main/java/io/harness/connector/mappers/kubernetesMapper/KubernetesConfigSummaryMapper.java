package io.harness.connector.mappers.kubernetesMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO;
import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO.KubernetesConfigSummaryDTOBuilder;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;

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
