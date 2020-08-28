package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerProbe;
import com.microsoft.azure.management.network.LoadBalancerTcpProbe;
import com.microsoft.azure.management.network.LoadBalancingRule;
import software.wings.beans.AzureConfig;

import java.util.List;

public interface AzureNetworkHelperServiceDelegate {
  /**
   * List load balancers by resource group.
   *
   * @param azureConfig
   * @param resourceGroupName
   * @return
   */
  List<LoadBalancer> listLoadBalancersByResourceGroup(AzureConfig azureConfig, String resourceGroupName);

  /**
   * List load balancer backend pools.
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param loadBalancerName
   * @return
   */
  List<LoadBalancerBackend> listLoadBalancerBackendPools(
      AzureConfig azureConfig, String resourceGroupName, String loadBalancerName);

  /**
   * List load balancer TCP probes.
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param loadBalancerName
   * @return
   */
  List<LoadBalancerTcpProbe> listLoadBalancerTcpProbes(
      AzureConfig azureConfig, String resourceGroupName, String loadBalancerName);

  /**
   * List backend pool rules.
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param loadBalancerName
   * @param backendPoolName
   * @return
   */
  List<LoadBalancingRule> listBackendPoolRules(
      AzureConfig azureConfig, String resourceGroupName, String loadBalancerName, String backendPoolName);

  /**
   * List backend pool probes.
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param loadBalancerName
   * @param backendPoolName
   * @return
   */
  List<LoadBalancerProbe> listBackendPoolProbes(
      AzureConfig azureConfig, String resourceGroupName, String loadBalancerName, String backendPoolName);
}
