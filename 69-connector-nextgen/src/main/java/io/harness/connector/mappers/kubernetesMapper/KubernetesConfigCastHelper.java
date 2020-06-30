package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.connector.common.kubernetes.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.connector.common.kubernetes.KubernetesCredentialType.MANUAL_CREDENTIALS;

import com.google.inject.Singleton;

import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesCredential;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesDelegateDetails;
import io.harness.exception.UnexpectedException;

@Singleton
public class KubernetesConfigCastHelper {
  public KubernetesDelegateDetails castToKubernetesDelegateCredential(KubernetesCredential kubernetesClusterConfig) {
    try {
      return (KubernetesDelegateDetails) kubernetesClusterConfig;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", INHERIT_FROM_DELEGATE),
          ex);
    }
  }

  public KubernetesClusterDetails castToManualKubernetesCredentials(KubernetesCredential kubernetesClusterConfig) {
    try {
      return (KubernetesClusterDetails) kubernetesClusterConfig;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", MANUAL_CREDENTIALS),
          ex);
    }
  }
}
