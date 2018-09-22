package software.wings.service.intfc;

import software.wings.beans.AzureKubernetesCluster;

import java.util.List;
import java.util.Map;

public interface AzureResourceService {
  Map<String, String> listSubscriptions(String cloudProviderId);

  List<String> listContainerRegistries(String cloudProviderId, String subscriptionId);

  List<String> listRepositories(String cloudProviderId, String subscriptionId, String registryName);

  List<String> listRepositoryTags(
      String cloudProviderId, String subscriptionId, String registryName, String repositoryName);

  List<AzureKubernetesCluster> listKubernetesClusters(String cloudProviderId, String subscriptionId);
}