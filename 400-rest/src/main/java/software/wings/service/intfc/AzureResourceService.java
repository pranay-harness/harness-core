/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.AzureImageGallery;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureResourceGroup;
import software.wings.beans.NameValuePair;

import java.util.List;
import java.util.Map;

public interface AzureResourceService {
  Map<String, String> listSubscriptions(String accountId, String cloudProviderId);

  List<String> listContainerRegistryNames(String cloudProviderId, String subscriptionId);

  List<AzureContainerRegistry> listContainerRegistries(String cloudProviderId, String subscriptionId);

  List<String> listRepositories(String cloudProviderId, String subscriptionId, String registryName);

  List<String> listRepositoryTags(
      String cloudProviderId, String subscriptionId, String registryName, String repositoryName);

  List<AzureKubernetesCluster> listKubernetesClusters(String cloudProviderId, String subscriptionId);

  List<AzureResourceGroup> listResourceGroups(String accountId, String cloudProviderId, String subscriptionId);

  List<AzureImageGallery> listImageGalleries(
      String accountId, String cloudProviderId, String subscriptionId, String resourceGroupName);

  List<AzureImageDefinition> listImageDefinitions(
      String accountId, String cloudProviderId, String subscriptionId, String resourceGroupName, String galleryName);

  /**
   * List available Azure regions without government ones.
   *
   * @return
   */
  List<NameValuePair> listAzureRegions();
}
