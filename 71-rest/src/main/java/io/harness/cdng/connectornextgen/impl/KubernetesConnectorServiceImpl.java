package io.harness.cdng.connectornextgen.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.connectornextgen.KubernetesValidationHelper;
import io.harness.cdng.connectornextgen.service.KubernetesConnectorService;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class KubernetesConnectorServiceImpl implements KubernetesConnectorService {
  @Inject KubernetesValidationHelper kubernetesValidationHelper;

  public boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfigDTO) {
    kubernetesValidationHelper.listControllers(kubernetesClusterConfigDTO);
    return true;
  }
}
