package io.harness.connector.mappers.kubernetesMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO;
import io.harness.connector.apis.dto.k8connector.KubernetesConfigSummaryDTO.KubernetesConfigSummaryDTOBuilder;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.exception.UnknownEnumTypeException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class KubernetesConfigSummaryMapper implements ConnectorConfigSummaryDTOMapper<KubernetesClusterConfig> {
  private KubernetesConfigCastHelper kubernetesConfigCastHelper;
  public KubernetesConfigSummaryDTO toConnectorConfigSummaryDTO(KubernetesClusterConfig connector) {
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
      throw new UnknownEnumTypeException("Kubernetes credential type",
          connector.getCredentialType() == null ? null : connector.getCredentialType().getDisplayName());
    }
  }
}
