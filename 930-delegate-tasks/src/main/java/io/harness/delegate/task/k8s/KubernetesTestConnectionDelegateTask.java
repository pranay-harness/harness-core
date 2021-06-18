package io.harness.delegate.task.k8s;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.K8sValidationParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class KubernetesTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private KubernetesValidationHandler kubernetesValidationHandler;

  public KubernetesTestConnectionDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public KubernetesConnectionTaskResponse run(TaskParameters parameters) {
    KubernetesConnectionTaskParams kubernetesConnectionTaskParams = (KubernetesConnectionTaskParams) parameters;
    final K8sValidationParams k8sValidationParams =
        K8sValidationParams.builder()
            .encryptedDataDetails(kubernetesConnectionTaskParams.getEncryptionDetails())
            .kubernetesClusterConfigDTO(kubernetesConnectionTaskParams.getKubernetesClusterConfig())
            .build();
    ConnectorValidationResult connectorValidationResult =
        kubernetesValidationHandler.validate(k8sValidationParams, getAccountId());
    connectorValidationResult.setDelegateId(getDelegateId());
    return KubernetesConnectionTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }

  @Override
  public KubernetesConnectionTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
