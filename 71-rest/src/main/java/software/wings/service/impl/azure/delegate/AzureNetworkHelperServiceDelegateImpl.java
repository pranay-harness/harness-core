package software.wings.service.impl.azure.delegate;

import static io.harness.azure.model.AzureConstants.AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.BACKEND_POOL_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.LOAD_BALANCER_NAME_NULL_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerProbe;
import com.microsoft.azure.management.network.LoadBalancerTcpProbe;
import com.microsoft.azure.management.network.LoadBalancingRule;
import io.fabric8.utils.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.azure.delegate.AzureNetworkHelperServiceDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureNetworkHelperServiceDelegateImpl
    extends AzureHelperService implements AzureNetworkHelperServiceDelegate {
  @Override
  public Optional<LoadBalancer> getLoadBalancerByName(
      AzureConfig azureConfig, final String resourceGroupName, final String loadBalancerName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(loadBalancerName)) {
      throw new IllegalArgumentException(LOAD_BALANCER_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start getting load balancer by resourceGroupName: {}, loadBalancerName: {}", resourceGroupName,
        loadBalancerName);
    LoadBalancer loadBalancer = azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName);

    if (isNull(loadBalancer)) {
      return Optional.empty();
    }

    return Optional.of(loadBalancer);
  }

  @Override
  public List<LoadBalancer> listLoadBalancersByResourceGroup(AzureConfig azureConfig, final String resourceGroupName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing load balancers by resourceGroupName {}", resourceGroupName);
    PagedList<LoadBalancer> loadBalancers = azure.loadBalancers().listByResourceGroup(resourceGroupName);
    List<LoadBalancer> loadBalancersList = new ArrayList<>();
    for (LoadBalancer loadBalancer : loadBalancers) {
      loadBalancersList.add(loadBalancer);
    }
    return loadBalancersList;
  }

  @Override
  public List<LoadBalancerBackend> listLoadBalancerBackendPools(
      AzureConfig azureConfig, final String resourceGroupName, final String loadBalancerName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(loadBalancerName)) {
      throw new IllegalArgumentException(LOAD_BALANCER_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing load balancer backend pools by resourceGroupName {}, loadBalancerName: {}",
        resourceGroupName, loadBalancerName);
    LoadBalancer loadBalancer = azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName);
    if (loadBalancer == null) {
      return Collections.emptyList();
    }

    return new ArrayList<>(loadBalancer.backends().values());
  }

  @Override
  public List<LoadBalancerTcpProbe> listLoadBalancerTcpProbes(
      AzureConfig azureConfig, final String resourceGroupName, final String loadBalancerName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(loadBalancerName)) {
      throw new IllegalArgumentException(LOAD_BALANCER_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug("Start listing load balancer TCP probes for loadBalancerName {}, resourceGroupName: {}",
        loadBalancerName, resourceGroupName);
    LoadBalancer loadBalancer = azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName);
    if (loadBalancer == null) {
      return Collections.emptyList();
    }

    return new ArrayList<>(loadBalancer.tcpProbes().values());
  }

  @Override
  public List<LoadBalancingRule> listBackendPoolRules(AzureConfig azureConfig, final String resourceGroupName,
      final String loadBalancerName, final String backendPoolName) {
    Optional<LoadBalancerBackend> loadBalancerBackendPoolOp =
        getLoadBalancerBackendPool(azureConfig, resourceGroupName, loadBalancerName, backendPoolName);

    if (!loadBalancerBackendPoolOp.isPresent()) {
      return Collections.emptyList();
    }

    logger.debug("Start listing backend pool rules for backendPoolName {}, loadBalancerName {}, resourceGroupName: {}",
        backendPoolName, loadBalancerName, resourceGroupName);
    return new ArrayList<>(loadBalancerBackendPoolOp.get().loadBalancingRules().values());
  }

  @Override
  public List<LoadBalancerProbe> listBackendPoolProbes(AzureConfig azureConfig, final String resourceGroupName,
      final String loadBalancerName, final String backendPoolName) {
    Optional<LoadBalancerBackend> loadBalancerBackendPoolOp =
        getLoadBalancerBackendPool(azureConfig, resourceGroupName, loadBalancerName, backendPoolName);

    logger.debug("Start listing backend pool probes for backendPoolName {}, loadBalancerName {}, resourceGroupName: {}",
        backendPoolName, loadBalancerName, resourceGroupName);
    return loadBalancerBackendPoolOp.map(this ::toLoadBalancerBackendProbes).orElse(Collections.emptyList());
  }

  @NotNull
  private List<LoadBalancerProbe> toLoadBalancerBackendProbes(LoadBalancerBackend loadBalancerBackend) {
    return loadBalancerBackend.loadBalancingRules()
        .values()
        .stream()
        .map(LoadBalancingRule::probe)
        .collect(Collectors.toList());
  }

  public Optional<LoadBalancerBackend> getLoadBalancerBackendPool(
      AzureConfig azureConfig, String resourceGroupName, String loadBalancerName, String backendPoolName) {
    if (isBlank(resourceGroupName)) {
      throw new IllegalArgumentException(RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(loadBalancerName)) {
      throw new IllegalArgumentException(LOAD_BALANCER_NAME_NULL_VALIDATION_MSG);
    }
    if (isBlank(backendPoolName)) {
      throw new IllegalArgumentException(BACKEND_POOL_NAME_NULL_VALIDATION_MSG);
    }

    Azure azure = getAzureClient(azureConfig);
    Objects.notNull(azure, AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG);

    logger.debug(
        "Start getting load balancer backend pool, backendPoolName {}, loadBalancerName {}, resourceGroupName: {}",
        backendPoolName, loadBalancerName, resourceGroupName);
    LoadBalancerBackend loadBalancerBackendPool =
        azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName).backends().get(backendPoolName);

    if (loadBalancerBackendPool == null) {
      return Optional.empty();
    }

    return Optional.of(loadBalancerBackendPool);
  }
}
