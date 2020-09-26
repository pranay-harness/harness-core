package io.harness.cdng.connector.tasks;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.exception.UnexpectedException;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class KubernetesConnectionDelegateValidation extends AbstractDelegateValidateTask {
  public KubernetesConnectionDelegateValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    KubernetesClusterConfigDTO kubernetesClusterConfig;
    if (getParameters()[0] instanceof KubernetesConnectionTaskParams) {
      KubernetesConnectionTaskParams kubernetesConnectionTaskParams =
          (KubernetesConnectionTaskParams) getParameters()[0];
      kubernetesClusterConfig = kubernetesConnectionTaskParams.getKubernetesClusterConfig();
    } else {
      throw new UnexpectedException("INVALID PARAMETER: Expecting the input of type KubernetesConnectionTaskParams");
    }
    boolean validated = false;
    if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      validated = ((KubernetesDelegateDetailsDTO) kubernetesClusterConfig.getCredential().getConfig())
                      .getDelegateName()
                      .equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      String url;
      url = "None".equals(
                ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig()).getMasterUrl())
          ? "https://container.googleapis.com/"
          : ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig()).getMasterUrl();
      validated = connectableHttpUrl(url);
    }
    return singletonList(DelegateConnectionResult.builder().criteria("").validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    KubernetesClusterConfigDTO kubernetesClusterConfig;
    if (getParameters()[0] instanceof KubernetesConnectionTaskParams) {
      KubernetesConnectionTaskParams kubernetesConnectionTaskParams =
          (KubernetesConnectionTaskParams) getParameters()[0];
      kubernetesClusterConfig = kubernetesConnectionTaskParams.getKubernetesClusterConfig();
    } else {
      throw new UnexpectedException("INVALID PARAMETER: Expecting the input of type KubernetesConnectionTaskParams");
    }
    if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO k8sDelegateDetails =
          (KubernetesDelegateDetailsDTO) kubernetesClusterConfig.getCredential().getConfig();
      return Collections.singletonList(k8sDelegateDetails.getDelegateName());
    } else if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      String masterUrl =
          ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig()).getMasterUrl();
      return Collections.singletonList(masterUrl);
    } else {
      throw new UnsupportedOperationException(String.format("Invalid kubernetes cofing type [%s]",
          kubernetesClusterConfig.getCredential().getKubernetesCredentialType()));
    }
  }
}
