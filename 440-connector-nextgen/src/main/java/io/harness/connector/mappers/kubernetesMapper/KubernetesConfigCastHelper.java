/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;

import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesCredential;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.exception.UnexpectedException;

import com.google.inject.Singleton;

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
