package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Helper class to interact with K8 cluster for creation/deletion of K8 entities.
 */

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GitConfig;
import software.wings.beans.container.ImageDetails;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Singleton
public class CIK8CtlHandler {
  @Inject private SecretSpecBuilder secretSpecBuilder;
  @Inject Provider<ExecCommandListener> execListenerProvider;

  public void createRegistrySecret(KubernetesClient kubernetesClient, String namespace, ImageDetails imageDetails) {
    Secret secret = secretSpecBuilder.getRegistrySecretSpec(imageDetails, namespace);
    if (secret != null) {
      kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret);
    }
  }

  public Pod createPod(KubernetesClient kubernetesClient, Pod pod) {
    return kubernetesClient.pods().create(pod);
  }

  public Boolean deletePod(KubernetesClient kubernetesClient, String podName, String namespace) {
    return kubernetesClient.pods().inNamespace(namespace).withName(podName).delete();
  }

  public void createGitSecret(KubernetesClient kubernetesClient, String namespace, GitConfig gitConfig,
      List<EncryptedDataDetail> gitEncryptedDataDetails) throws UnsupportedEncodingException {
    Secret secret = secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    if (secret != null) {
      kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret);
    }
  }

  /**
   * Executes a command or a list of commands on a container in a pod.
   */
  public boolean executeCommand(KubernetesClient kubernetesClient, String podName, String containerName,
      String namespace, String[] commands, Integer timeoutSecs) throws InterruptedException, TimeoutException {
    ExecCommandListener execListener = execListenerProvider.get();
    ExecWatch watch = kubernetesClient.pods()
                          .inNamespace(namespace)
                          .withName(podName)
                          .inContainer(containerName)
                          .redirectingInput()
                          .usingListener(execListener)
                          .exec(commands);
    return execListener.getReturnStatus(watch, timeoutSecs);
  }
}
