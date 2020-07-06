package io.harness.cdng.connectornextgen.tasks;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesDelegateDetailsDTO;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class KubernetesConnectionDelegateValidation extends AbstractDelegateValidateTask {
  public KubernetesConnectionDelegateValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    KubernetesClusterConfigDTO kubernetesClusterConfig = (KubernetesClusterConfigDTO) getParameters()[2];
    boolean validated = false;
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      validated = ((KubernetesDelegateDetailsDTO) kubernetesClusterConfig.getConfig())
                      .getDelegateName()
                      .equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      String url;
      url = "None".equals(((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig()).getMasterUrl())
          ? "https://container.googleapis.com/"
          : ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig()).getMasterUrl();
      validated = connectableHttpUrl(url);
    }
    return singletonList(DelegateConnectionResult.builder().criteria("").validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    KubernetesClusterConfigDTO kubernetesClusterConfig = (KubernetesClusterConfigDTO) getParameters()[2];
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      String delegateName = ((KubernetesDelegateDetailsDTO) kubernetesClusterConfig.getConfig()).getDelegateName();
      return Collections.singletonList(delegateName);
    } else if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      String masterUrl = ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig()).getMasterUrl();
      return Collections.singletonList(masterUrl);
    } else {
      throw new UnsupportedOperationException(
          String.format("Invalid kubernetes cofing type [%s]", kubernetesClusterConfig.getKubernetesCredentialType()));
    }
  }
}
