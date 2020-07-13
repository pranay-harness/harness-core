package io.harness.connector.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class KubernetesConnectionValidator {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;

  public ConnectorValidationResult validate(KubernetesClusterConfigDTO kubernetesClusterConfig, String accountId) {
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      return ConnectorValidationResult.builder().valid(true).build();
    }

    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    TaskData taskData =
        TaskData.builder()
            .async(false)
            .taskType("VALIDATE_KUBERNETES_CONFIG")
            .parameters(new Object[] {
                KubernetesConnectionTaskParams.builder().kubernetesClusterConfig(kubernetesClusterConfig).build()})
            .timeout(TimeUnit.MINUTES.toMillis(1))
            .build();
    KubernetesConnectionTaskResponse responseData =
        managerDelegateServiceDriver.sendTask(accountId, setupAbstractions, taskData);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }
}
