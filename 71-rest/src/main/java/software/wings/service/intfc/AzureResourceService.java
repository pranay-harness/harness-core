package software.wings.service.intfc;

import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.NameValuePair;

import java.util.List;
import java.util.Map;

public interface AzureResourceService {
  Map<String, String> listSubscriptions(String cloudProviderId);

  List<String> listContainerRegistryNames(String cloudProviderId, String subscriptionId);

  List<AzureContainerRegistry> listContainerRegistries(String cloudProviderId, String subscriptionId);

  List<String> listRepositories(String cloudProviderId, String subscriptionId, String registryName);

  List<String> listRepositoryTags(
      String cloudProviderId, String subscriptionId, String registryName, String repositoryName);

  List<AzureKubernetesCluster> listKubernetesClusters(String cloudProviderId, String subscriptionId);

  /**
   * List available Azure regions without government ones.
   *
   * @return
   */
  List<NameValuePair> listAzureRegions();
}
