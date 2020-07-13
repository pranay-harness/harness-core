package io.harness.cdng.connectornextgen.tasks;

import com.google.inject.Inject;

import io.harness.cdng.connectornextgen.service.KubernetesConnectorDelegateService;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class KubernetesTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private KubernetesConnectorDelegateService kubernetesConnectorDelegateService;
  private static final String EMPTY_STR = "";

  public KubernetesTestConnectionDelegateTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public KubernetesConnectionTaskResponse run(TaskParameters parameters) {
    KubernetesConnectionTaskParams kubernetesConnectionTaskParams = (KubernetesConnectionTaskParams) parameters;
    KubernetesClusterConfigDTO kubernetesClusterConfig = kubernetesConnectionTaskParams.getKubernetesClusterConfig();
    Exception execptionInProcessing = null;
    boolean validCredentials = false;
    try {
      validCredentials = kubernetesConnectorDelegateService.validate(kubernetesClusterConfig);
    } catch (Exception ex) {
      logger.info("Exception while validating kubernetes credentials", ex);
      execptionInProcessing = ex;
    }
    return KubernetesConnectionTaskResponse.builder()
        .connectionSuccessFul(validCredentials)
        .errorMessage(execptionInProcessing != null ? execptionInProcessing.getMessage() : EMPTY_STR)
        .build();
  }

  @Override
  public KubernetesConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
