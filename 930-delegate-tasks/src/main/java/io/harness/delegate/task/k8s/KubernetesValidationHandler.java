package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.k8Connector.K8sValidationParams;
import io.harness.connector.ConnectorValidationHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CDP)
public class KubernetesValidationHandler implements ConnectorValidationHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final K8sValidationParams k8sValidationParams = (K8sValidationParams) connectorValidationParams;
    return k8sTaskHelperBase.validate(
        k8sValidationParams.getKubernetesClusterConfigDTO(), k8sValidationParams.getEncryptedDataDetails());
  }
}