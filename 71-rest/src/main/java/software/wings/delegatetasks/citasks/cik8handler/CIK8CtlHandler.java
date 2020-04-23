package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Helper class to interact with K8 cluster for creation/deletion of K8 entities.
 */

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GitConfig;
import software.wings.beans.container.ImageDetails;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Singleton
public class CIK8CtlHandler {
  @Inject private SecretSpecBuilder secretSpecBuilder;

  public void createRegistrySecret(KubernetesClient kubernetesClient, String namespace, ImageDetails imageDetails) {
    Secret secret = secretSpecBuilder.getRegistrySecretSpec(imageDetails, namespace);
    if (secret != null) {
      kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret);
    }
  }

  public Pod createPod(KubernetesClient kubernetesClient, Pod pod) {
    return kubernetesClient.pods().create(pod);
  }

  public void createGitSecret(KubernetesClient kubernetesClient, String namespace, GitConfig gitConfig,
      List<EncryptedDataDetail> gitEncryptedDataDetails) throws UnsupportedEncodingException {
    Secret secret = secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    if (secret != null) {
      kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret);
    }
  }
}
