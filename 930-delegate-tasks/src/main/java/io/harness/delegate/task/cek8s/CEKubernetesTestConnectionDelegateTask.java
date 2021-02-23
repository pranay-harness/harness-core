package io.harness.delegate.task.cek8s;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
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
import org.apache.commons.lang3.NotImplementedException;

public class CEKubernetesTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private CEKubernetesValidationHandler ceKubernetesValidationHandler;

  public CEKubernetesTestConnectionDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    KubernetesConnectionTaskParams kubernetesConnectionTaskParams = (KubernetesConnectionTaskParams) parameters;
    final K8sValidationParams k8sValidationParams =
        K8sValidationParams.builder()
            .encryptedDataDetails(kubernetesConnectionTaskParams.getEncryptionDetails())
            .kubernetesClusterConfigDTO(kubernetesConnectionTaskParams.getKubernetesClusterConfig())
            .build();
    ConnectorValidationResult connectorValidationResult =
        ceKubernetesValidationHandler.validate(k8sValidationParams, getAccountId());
    connectorValidationResult.setDelegateId(getDelegateId());
    return KubernetesConnectionTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }
}
