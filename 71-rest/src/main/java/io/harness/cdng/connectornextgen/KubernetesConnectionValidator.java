package io.harness.cdng.connectornextgen;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.connectornextgen.service.KubernetesConnectorService;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class KubernetesConnectionValidator {
  private DelegateProxyFactory delegateProxyFactory;

  public boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfig, String accountId) {
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      return true;
    }
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    KubernetesConnectorService kubernetesConnectorService =
        delegateProxyFactory.get(KubernetesConnectorService.class, syncTaskContext);
    try {
      Boolean kubernetesConnectionResponse = kubernetesConnectorService.validate(kubernetesClusterConfig);
      return kubernetesConnectionResponse;
    } catch (Exception e) {
      logger.info("KUBERNETES VALIDATION EXCEPTION: Exception in validating the kubernetes credentials", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }
}
