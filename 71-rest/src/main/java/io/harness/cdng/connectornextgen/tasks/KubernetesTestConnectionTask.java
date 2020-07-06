package io.harness.cdng.connectornextgen.tasks;

import com.google.inject.Inject;

import io.harness.cdng.connectornextgen.service.KubernetesConnectorService;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.RemoteMethodReturnValueData;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class KubernetesTestConnectionTask extends AbstractDelegateRunnableTask {
  @Inject private KubernetesConnectorService kubernetesConnectorService;

  public KubernetesTestConnectionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public RemoteMethodReturnValueData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public RemoteMethodReturnValueData run(Object[] parameters) {
    KubernetesClusterConfigDTO kubernetesClusterConfig = (KubernetesClusterConfigDTO) getParameters()[2];
    Exception execptionInProcessing = null;
    boolean validCredentials = false;
    try {
      validCredentials = kubernetesConnectorService.validate(kubernetesClusterConfig);
    } catch (Exception ex) {
      logger.info("Exception while validating kubernetes credentials", ex);
      execptionInProcessing = ex;
    }
    return RemoteMethodReturnValueData.builder().returnValue(validCredentials).exception(execptionInProcessing).build();
  }
}
