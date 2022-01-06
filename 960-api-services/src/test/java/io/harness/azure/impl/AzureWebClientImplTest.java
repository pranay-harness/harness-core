/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.azure.AzureClient;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.util.concurrent.TimeLimiter;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApps;
import com.microsoft.rest.LogLevel;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Completable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureClient.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureWebClientImplTest extends CategoryTest {
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks AzureWebClientImpl azureWebClientImpl;

  @Before
  public void before() throws Exception {
    ApplicationTokenCredentials tokenCredentials = mock(ApplicationTokenCredentials.class);
    PowerMockito.whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");
    PowerMockito.mockStatic(Azure.class);
    when(Azure.configure()).thenReturn(configurable);
    when(configurable.withLogLevel(Matchers.any(LogLevel.class))).thenReturn(configurable);
    when(configurable.authenticate(Matchers.any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    when(authenticated.withSubscription(anyString())).thenReturn(azure);
    when(authenticated.withDefaultSubscription()).thenReturn(azure);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testListWebAppsByResourceGroupName() {
    AzureClientContext azureClientContext = buildAzureClientContext();
    WebApps mockWebApps = mockWebApps();
    mockWebAppsListByResourceGroup(mockWebApps);

    List<WebApp> response = azureWebClientImpl.listWebAppsByResourceGroupName(azureClientContext);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testListDeploymentSlotsByWebAppName() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    WebApps mockWebApps = mockWebApps();
    mockDeploymentSlotsList(mockWebApps);

    List<DeploymentSlot> response = azureWebClientImpl.listDeploymentSlotsByWebAppName(azureWebClientContext);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetWebAppByName() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    WebApps mockWebApps = mockWebApps();
    mockWebAppsGetByResourceGroup(mockWebApps);

    Optional<WebApp> response = azureWebClientImpl.getWebAppByName(azureWebClientContext);

    assertThat(response.isPresent()).isTrue();
    assertThat(response.get()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetDeploymentSlotByName() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);

    Optional<DeploymentSlot> response = azureWebClientImpl.getDeploymentSlotByName(azureWebClientContext, slotName);

    assertThat(response.isPresent()).isTrue();
    assertThat(response.get()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStartDeploymentSlot() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);
    doNothing().when(mockDeploymentSlot).start();

    azureWebClientImpl.startDeploymentSlot(azureWebClientContext, slotName);

    verify(mockDeploymentSlot, times(1)).start();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStartDeploymentSlotAsync() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);
    Completable mockCompletable = mock(Completable.class);
    doReturn(mockCompletable).when(mockDeploymentSlot).startAsync();

    Completable completable = azureWebClientImpl.startDeploymentSlotAsync(azureWebClientContext, slotName);

    verify(mockDeploymentSlot, times(1)).startAsync();
    assertThat(completable).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStartDeploymentSlotWithDeploymentSlot() {
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);
    doNothing().when(mockDeploymentSlot).start();

    azureWebClientImpl.startDeploymentSlot(mockDeploymentSlot);

    verify(mockDeploymentSlot, times(1)).start();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStartDeploymentSlotAsyncWithDeploymentSlot() {
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);
    Completable mockCompletable = mock(Completable.class);
    doReturn(mockCompletable).when(mockDeploymentSlot).startAsync();

    Completable completable = azureWebClientImpl.startDeploymentSlotAsync(mockDeploymentSlot);

    verify(mockDeploymentSlot, times(1)).startAsync();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStopDeploymentSlot() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);
    doNothing().when(mockDeploymentSlot).stop();

    azureWebClientImpl.stopDeploymentSlot(azureWebClientContext, slotName);

    verify(mockDeploymentSlot, times(1)).stop();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStopDeploymentSlotAsync() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);
    Completable mockCompletable = mock(Completable.class);
    doReturn(mockCompletable).when(mockDeploymentSlot).stopAsync();

    Completable completable = azureWebClientImpl.stopDeploymentSlotAsync(azureWebClientContext, slotName);

    verify(mockDeploymentSlot, times(1)).stopAsync();
    assertThat(completable).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStopDeploymentSlotWithDeploymentSlot() {
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);
    doNothing().when(mockDeploymentSlot).start();

    azureWebClientImpl.startDeploymentSlot(mockDeploymentSlot);

    verify(mockDeploymentSlot, times(1)).start();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testStopDeploymentSlotAsyncWithDeploymentSlot() {
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);
    Completable mockCompletable = mock(Completable.class);
    doReturn(mockCompletable).when(mockDeploymentSlot).stopAsync();

    Completable completable = azureWebClientImpl.stopDeploymentSlotAsync(mockDeploymentSlot);

    verify(mockDeploymentSlot, times(1)).stopAsync();
    assertThat(completable).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetSlotState() {
    AzureWebClientContext azureWebClientContext = buildAzureWebClientContext();
    String slotName = "slotName";
    String runningSlotState = "Running";
    WebApps mockWebApps = mockWebApps();
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mockDeploymentSlotsGetByName(mockDeploymentSlots, slotName);
    doReturn(runningSlotState).when(mockDeploymentSlot).state();

    String slotState = azureWebClientImpl.getSlotState(azureWebClientContext, slotName);

    assertThat(slotState).isNotNull();
    assertThat(slotState).isEqualTo(runningSlotState);
  }

  public DeploymentSlot mockDeploymentSlotsGetByName(DeploymentSlots mockDeploymentSlots, String slotName) {
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);
    when(mockDeploymentSlots.getByName(slotName)).thenReturn(mockDeploymentSlot);
    return mockDeploymentSlot;
  }

  public DeploymentSlots mockWebAppsDeploymentSlots(WebApp mockWebApp) {
    DeploymentSlots mockDeploymentSlots = mock(DeploymentSlots.class);
    when(mockWebApp.deploymentSlots()).thenReturn(mockDeploymentSlots);
    return mockDeploymentSlots;
  }

  public void mockDeploymentSlotsList(WebApps mockWebApps) {
    WebApp mockWebApp = mockWebAppsGetByResourceGroup(mockWebApps);
    DeploymentSlots mockDeploymentSlots = mockWebAppsDeploymentSlots(mockWebApp);
    DeploymentSlot mockDeploymentSlot = mock(DeploymentSlot.class);

    PagedList<DeploymentSlot> pageList = getPageList();
    pageList.add(mockDeploymentSlot);
    when(mockDeploymentSlots.list()).thenReturn(pageList);
  }

  public void mockWebAppsListByResourceGroup(WebApps mockWebApps) {
    WebApp mockWebApp = mock(WebApp.class);
    PagedList<WebApp> pageList = getPageList();
    pageList.add(mockWebApp);

    when(mockWebApps.listByResourceGroup("resourceGroupName")).thenReturn(pageList);
  }

  public WebApps mockWebApps() {
    WebApps mockWebApps = mock(WebApps.class);
    when(azure.webApps()).thenReturn(mockWebApps);
    return mockWebApps;
  }

  public WebApp mockWebAppsGetByResourceGroup(WebApps mockWebApps) {
    WebApp mockWebApp = mock(WebApp.class);
    when(mockWebApps.getByResourceGroup("resourceGroupName", "webAppName")).thenReturn(mockWebApp);
    return mockWebApp;
  }

  public AzureWebClientContext buildAzureWebClientContext() {
    return AzureWebClientContext.builder()
        .azureConfig(buildAzureConfig())
        .resourceGroupName("resourceGroupName")
        .subscriptionId("subscriptionId")
        .appName("webAppName")
        .build();
  }

  public AzureClientContext buildAzureClientContext() {
    return new AzureClientContext(buildAzureConfig(), "subscriptionId", "resourceGroupName");
  }

  private AzureConfig buildAzureConfig() {
    return AzureConfig.builder().key("key".toCharArray()).clientId("clientId").tenantId("tenantId").build();
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
}
