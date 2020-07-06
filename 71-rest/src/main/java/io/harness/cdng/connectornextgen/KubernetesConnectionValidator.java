package io.harness.cdng.connectornextgen;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class KubernetesConnectionValidator {
  private DelegateService delegateService;

  public boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfig, String accountId) {
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      return true;
    }
    try {
      KubernetesConnectionTaskResponse kubernetesConnectionTaskResponse = delegateService.executeTask(
          DelegateTask.builder()
              .accountId(accountId)
              .data(TaskData.builder()
                        .async(false)
                        .taskType(TaskType.VALIDATE_KUBERNETES_CONFIG.name())
                        .parameters(new Object[] {KubernetesConnectionTaskParams.builder()
                                                      .kubernetesClusterConfig(kubernetesClusterConfig)
                                                      .build()})
                        .timeout(TimeUnit.MINUTES.toMillis(2))
                        .build())
              .build());
      return kubernetesConnectionTaskResponse.getConnectionSuccessFul();
    } catch (Exception e) {
      logger.info("KUBERNETES VALIDATION EXCEPTION: Exception in validating the kubernetes credentials", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }
}
