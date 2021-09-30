package io.harness.delegate.task.cek8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.k8Connector.CEK8sValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEKubernetesValidationHandler implements ConnectorValidationHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final CEK8sValidationParams k8sValidationParams = (CEK8sValidationParams) connectorValidationParams;

    return k8sTaskHelperBase.validateCEKubernetesCluster(k8sValidationParams.getKubernetesClusterConfigDTO(),
        accountIdentifier, k8sValidationParams.getEncryptedDataDetails(), k8sValidationParams.getFeaturesEnabled());
  }
}
