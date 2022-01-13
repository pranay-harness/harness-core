/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import io.harness.CategoryTest;
import io.harness.azure.AzureClient;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;

import com.google.common.util.concurrent.TimeLimiter;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerProbe;
import com.microsoft.azure.management.network.LoadBalancerTcpProbe;
import com.microsoft.azure.management.network.LoadBalancers;
import com.microsoft.azure.management.network.LoadBalancingRule;
import com.microsoft.rest.LogLevel;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureClient.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureNetworkClientImplTest extends CategoryTest {
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks AzureNetworkClientImpl azureNetworkClient;

  @Before
  public void before() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");
    mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
    when(authenticated.withSubscription(anyString())).thenReturn(azure);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListLoadBalancersByResourceGroup() {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";

    LoadBalancers loadBalancers = mock(LoadBalancers.class);
    LoadBalancer loadBalancer = mock(LoadBalancer.class);
    PagedList<LoadBalancer> loadBalancersList = getPageList();
    loadBalancersList.add(loadBalancer);

    when(azure.loadBalancers()).thenReturn(loadBalancers);
    when(loadBalancers.listByResourceGroup(resourceGroupName)).thenReturn(loadBalancersList);

    List<LoadBalancer> response =
        azureNetworkClient.listLoadBalancersByResourceGroup(getAzureComputeConfig(), subscriptionId, resourceGroupName);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(LoadBalancer.class);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListLoadBalancerBackendPools() {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String loadBalancerName = "loadBalancerName";

    LoadBalancers loadBalancers = mock(LoadBalancers.class);
    LoadBalancer loadBalancer = mock(LoadBalancer.class);
    LoadBalancerBackend loadBalancerBackend = mock(LoadBalancerBackend.class);

    when(azure.loadBalancers()).thenReturn(loadBalancers);
    when(loadBalancers.getByResourceGroup(resourceGroupName, loadBalancerName)).thenReturn(loadBalancer);
    when(loadBalancer.backends()).thenReturn(new HashMap<String, LoadBalancerBackend>() {
      { put("loadBalancerBackendKey", loadBalancerBackend); }
    });

    List<LoadBalancerBackend> response = azureNetworkClient.listLoadBalancerBackendPools(
        getAzureComputeConfig(), subscriptionId, resourceGroupName, loadBalancerName);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(LoadBalancerBackend.class);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListLoadBalancerTcpProbes() {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String loadBalancerName = "loadBalancerName";

    LoadBalancers loadBalancers = mock(LoadBalancers.class);
    LoadBalancer loadBalancer = mock(LoadBalancer.class);
    LoadBalancerTcpProbe loadBalancerTcpProbe = mock(LoadBalancerTcpProbe.class);

    when(azure.loadBalancers()).thenReturn(loadBalancers);
    when(loadBalancers.getByResourceGroup(resourceGroupName, loadBalancerName)).thenReturn(loadBalancer);
    when(loadBalancer.tcpProbes()).thenReturn(new HashMap<String, LoadBalancerTcpProbe>() {
      { put("loadBalancerTcpProbeKey", loadBalancerTcpProbe); }
    });

    List<LoadBalancerTcpProbe> response = azureNetworkClient.listLoadBalancerTcpProbes(
        getAzureComputeConfig(), subscriptionId, resourceGroupName, loadBalancerName);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(LoadBalancerTcpProbe.class);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListBackendPoolRules() {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String loadBalancerName = "loadBalancerName";
    String backendPoolName = "backendPoolName";

    LoadBalancers loadBalancers = mock(LoadBalancers.class);
    LoadBalancer loadBalancer = mock(LoadBalancer.class);
    LoadBalancerBackend loadBalancerBackend = mock(LoadBalancerBackend.class);
    LoadBalancingRule loadBalancingRule = mock(LoadBalancingRule.class);

    when(azure.loadBalancers()).thenReturn(loadBalancers);
    when(loadBalancers.getByResourceGroup(resourceGroupName, loadBalancerName)).thenReturn(loadBalancer);
    when(loadBalancer.backends()).thenReturn(new HashMap<String, LoadBalancerBackend>() {
      { put(backendPoolName, loadBalancerBackend); }
    });
    when(loadBalancerBackend.loadBalancingRules()).thenReturn(new HashMap<String, LoadBalancingRule>() {
      { put("loadBalancingRule", loadBalancingRule); }
    });

    List<LoadBalancingRule> response = azureNetworkClient.listBackendPoolRules(
        getAzureComputeConfig(), subscriptionId, resourceGroupName, loadBalancerName, backendPoolName);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(LoadBalancingRule.class);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListBackendPoolProbes() {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String loadBalancerName = "loadBalancerName";
    String backendPoolName = "backendPoolName";

    LoadBalancers loadBalancers = mock(LoadBalancers.class);
    LoadBalancer loadBalancer = mock(LoadBalancer.class);
    LoadBalancerBackend loadBalancerBackend = mock(LoadBalancerBackend.class);
    LoadBalancingRule loadBalancingRule = mock(LoadBalancingRule.class);
    LoadBalancerProbe loadBalancerProbe = mock(LoadBalancerProbe.class);

    when(azure.loadBalancers()).thenReturn(loadBalancers);
    when(loadBalancers.getByResourceGroup(resourceGroupName, loadBalancerName)).thenReturn(loadBalancer);
    when(loadBalancer.backends()).thenReturn(new HashMap<String, LoadBalancerBackend>() {
      { put(backendPoolName, loadBalancerBackend); }
    });
    when(loadBalancerBackend.loadBalancingRules()).thenReturn(new HashMap<String, LoadBalancingRule>() {
      { put("loadBalancingRule", loadBalancingRule); }
    });
    when(loadBalancingRule.probe()).thenReturn(loadBalancerProbe);

    List<LoadBalancerProbe> response = azureNetworkClient.listBackendPoolProbes(
        getAzureComputeConfig(), subscriptionId, resourceGroupName, loadBalancerName, backendPoolName);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0)).isInstanceOf(LoadBalancerProbe.class);
  }

  @NotNull
  public <T> PagedList<T> getPageList() {
    return new PagedList<T>() {
      @Override
      public Page<T> nextPage(String s) {
        return new Page<T>() {
          @Override
          public String nextPageLink() {
            return null;
          }
          @Override
          public List<T> items() {
            return null;
          }
        };
      }
    };
  }

  private AzureConfig getAzureComputeConfig() {
    return AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();
  }
}
