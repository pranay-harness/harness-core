package io.harness.cdng.connector.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.connector.KubernetesValidationHelper;
import io.harness.cdng.connector.service.KubernetesConnectorDelegateService;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class KubernetesConnectorDelegateServiceImpl implements KubernetesConnectorDelegateService {
  @Inject KubernetesValidationHelper kubernetesValidationHelper;

  public boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfigDTO) {
    kubernetesValidationHelper.listControllers(kubernetesClusterConfigDTO);
    return true;
  }
}
