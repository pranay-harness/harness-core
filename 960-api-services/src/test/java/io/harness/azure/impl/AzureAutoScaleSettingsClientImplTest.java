/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static org.mockito.Matchers.anyString;

import io.harness.CategoryTest;
import io.harness.azure.AzureClient;
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
import com.microsoft.azure.management.monitor.AutoscaleProfile;
import com.microsoft.azure.management.monitor.AutoscaleSetting;
import com.microsoft.azure.management.monitor.AutoscaleSettings;
import com.microsoft.azure.management.monitor.ScaleCapacity;
import com.microsoft.azure.management.monitor.implementation.AutoscaleSettingResourceInner;
import com.microsoft.azure.management.monitor.implementation.AutoscaleSettingsInner;
import com.microsoft.rest.LogLevel;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureClient.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureAutoScaleSettingsClientImplTest extends CategoryTest {
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks AzureAutoScaleSettingsClientImpl azureAutoScaleSettingsClient;

  @Before
  public void before() throws Exception {
    ApplicationTokenCredentials tokenCredentials = Mockito.mock(ApplicationTokenCredentials.class);
    PowerMockito.whenNew(ApplicationTokenCredentials.class).withAnyArguments().thenReturn(tokenCredentials);
    Mockito.when(tokenCredentials.getToken(anyString())).thenReturn("tokenValue");
    PowerMockito.mockStatic(Azure.class);
    Mockito.when(Azure.configure()).thenReturn(configurable);
    Mockito.when(configurable.withLogLevel(Matchers.any(LogLevel.class))).thenReturn(configurable);
    Mockito.when(configurable.authenticate(Matchers.any(ApplicationTokenCredentials.class))).thenReturn(authenticated);
    Mockito.when(authenticated.withSubscription(anyString())).thenReturn(azure);
    Mockito.when(authenticated.withDefaultSubscription()).thenReturn(azure);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetAutoScaleSettingByTargetResourceId() throws Exception {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    Optional<AutoscaleSetting> response = azureAutoScaleSettingsClient.getAutoScaleSettingByTargetResourceId(
        azureConfig, subscriptionId, resourceGroupName, targetResourceId);

    response.ifPresent(as -> Assertions.assertThat(as).isNotNull());
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testAttachAutoScaleSettingToTargetResourceId() throws Exception {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    String autoScaleSettingsJSON = "{\n"
        + "    \"location\": \"eastus\",\n"
        + "    \"tags\": {},\n"
        + "    \"properties\": {\n"
        + "        \"name\": \"testAutoScaleName\",\n"
        + "        \"enabled\": true,\n"
        + "        \"targetResourceUri\": \"targetResourceId\",\n"
        + "        \"profiles\": [\n"
        + "            {\n"
        + "                \"name\": \"Auto created scale condition\",\n"
        + "                \"capacity\": {\n"
        + "                    \"minimum\": \"2\",\n"
        + "                    \"maximum\": \"2\",\n"
        + "                    \"default\": \"2\"\n"
        + "                },\n"
        + "                \"rules\": [],\n"
        + "                \"fixedDate\": {\n"
        + "                    \"timeZone\": \"UTC\",\n"
        + "                    \"start\": \"2020-07-05T00:00:00.000Z\",\n"
        + "                    \"end\": \"2020-07-05T23:59:00.000Z\"\n"
        + "                }\n"
        + "            },\n"
        + "            {\n"
        + "                \"name\": \"Profile 2\",\n"
        + "                \"capacity\": {\n"
        + "                    \"minimum\": \"1\",\n"
        + "                    \"maximum\": \"1\",\n"
        + "                    \"default\": \"1\"\n"
        + "                },\n"
        + "                \"rules\": [],\n"
        + "                \"fixedDate\": {\n"
        + "                    \"timeZone\": \"UTC\",\n"
        + "                    \"start\": \"2020-08-18T00:00:00.000Z\",\n"
        + "                    \"end\": \"2020-08-18T23:59:00.000Z\"\n"
        + "                }\n"
        + "            },\n"
        + "            {\n"
        + "                \"name\": \"Profile 3\",\n"
        + "                \"capacity\": {\n"
        + "                    \"minimum\": \"1\",\n"
        + "                    \"maximum\": \"3\",\n"
        + "                    \"default\": \"2\"\n"
        + "                },\n"
        + "                \"rules\": [\n"
        + "                    {\n"
        + "                        \"scaleAction\": {\n"
        + "                            \"direction\": \"Increase\",\n"
        + "                            \"type\": \"ChangeCount\",\n"
        + "                            \"value\": \"1\",\n"
        + "                            \"cooldown\": \"PT5M\"\n"
        + "                        },\n"
        + "                        \"metricTrigger\": {\n"
        + "                            \"metricName\": \"Percentage CPU\",\n"
        + "                            \"metricNamespace\": \"\",\n"
        + "                            \"metricResourceUri\": \"metricResourceUri\",\n"
        + "                            \"operator\": \"GreaterThan\",\n"
        + "                            \"statistic\": \"Average\",\n"
        + "                            \"threshold\": 70,\n"
        + "                            \"timeAggregation\": \"Average\",\n"
        + "                            \"timeGrain\": \"PT1M\",\n"
        + "                            \"timeWindow\": \"PT10M\",\n"
        + "                            \"Dimensions\": [],\n"
        + "                            \"dividePerInstance\": false\n"
        + "                        }\n"
        + "                    }\n"
        + "                ]\n"
        + "            }\n"
        + "        ],\n"
        + "        \"notifications\": [],\n"
        + "        \"targetResourceLocation\": \"eastus\"\n"
        + "    },\n"
        + "    \"id\": \"AutoScaleSettingsId\",\n"
        + "    \"name\": \"AutoScaleSettingsName\",\n"
        + "    \"type\": \"Microsoft.Insights/autoscaleSettings\"\n"
        + "}";
    List<String> autoScaleSettingsJSONs = Collections.singletonList(autoScaleSettingsJSON);
    ScaleCapacity defaultProfileScalePolicy = new ScaleCapacity();
    defaultProfileScalePolicy.withMaximum("1");
    defaultProfileScalePolicy.withMinimum("0");
    defaultProfileScalePolicy.withDefaultProperty("1");
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    AutoscaleSettings mockAutoScaleSettings =
        mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);
    AutoscaleSettingResourceInner mockAutoScaleSettingResourceInner = Mockito.mock(AutoscaleSettingResourceInner.class);
    AutoscaleSettingsInner mockAutoScaleSettingsInner = Mockito.mock(AutoscaleSettingsInner.class);

    Mockito.when(mockAutoScaleSettings.inner()).thenReturn(mockAutoScaleSettingsInner);
    ArgumentCaptor<AutoscaleSettingResourceInner> autoScaleSettingResourceInnerCapture =
        ArgumentCaptor.forClass(AutoscaleSettingResourceInner.class);

    Mockito.doReturn(mockAutoScaleSettingResourceInner)
        .when(mockAutoScaleSettingsInner)
        .createOrUpdate(Matchers.eq(resourceGroupName), anyString(), autoScaleSettingResourceInnerCapture.capture());

    azureAutoScaleSettingsClient.attachAutoScaleSettingToTargetResourceId(azureConfig, subscriptionId,
        resourceGroupName, targetResourceId, autoScaleSettingsJSONs, defaultProfileScalePolicy);

    AutoscaleSettingResourceInner autoScaleSettingResult = autoScaleSettingResourceInnerCapture.getValue();
    Assertions.assertThat(autoScaleSettingResult).isNotNull();
    Assertions.assertThat(autoScaleSettingResult.targetResourceUri()).isEqualTo(targetResourceId);
    Assertions.assertThat(autoScaleSettingResult.profiles().size()).isEqualTo(3);
    Assertions.assertThat(autoScaleSettingResult.profiles().get(0).capacity().minimum())
        .isEqualTo(defaultProfileScalePolicy.minimum());
    Assertions.assertThat(autoScaleSettingResult.profiles().get(0).capacity().defaultProperty())
        .isEqualTo(defaultProfileScalePolicy.defaultProperty());
    Assertions.assertThat(autoScaleSettingResult.profiles().get(0).capacity().maximum())
        .isEqualTo(defaultProfileScalePolicy.maximum());
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testClearAutoScaleSettingOnTargetResourceId() throws Exception {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    AutoscaleSettings mockAutoScaleSettings =
        mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    ArgumentCaptor<String> autoScaleSettingIdCapture = ArgumentCaptor.forClass(String.class);
    Mockito.doNothing().when(mockAutoScaleSettings).deleteById(autoScaleSettingIdCapture.capture());

    azureAutoScaleSettingsClient.clearAutoScaleSettingOnTargetResourceId(
        azureConfig, subscriptionId, resourceGroupName, targetResourceId);

    String autoScaleSettingIdResult = autoScaleSettingIdCapture.getValue();
    Assertions.assertThat(autoScaleSettingIdResult).isNotNull();
    Assertions.assertThat(autoScaleSettingIdResult).isEqualTo(autoScaleSettingId);
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testGetDefaultAutoScaleProfile() throws Exception {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    Optional<AutoscaleProfile> response = azureAutoScaleSettingsClient.getDefaultAutoScaleProfile(
        azureConfig, subscriptionId, resourceGroupName, targetResourceId);

    response.ifPresent(as -> Assertions.assertThat(as).isNotNull());
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testListAutoScaleProfilesByTargetResourceId() throws Exception {
    String subscriptionId = "subscriptionId";
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    List<AutoscaleProfile> response = azureAutoScaleSettingsClient.listAutoScaleProfilesByTargetResourceId(
        azureConfig, subscriptionId, resourceGroupName, targetResourceId);

    Assertions.assertThat(response).isNotNull();
    Assertions.assertThat(response.size()).isEqualTo(1);
  }

  public AutoscaleSettings mockAutosScaleSettings(
      String resourceGroupName, String targetResourceId, String autoScaleSettingId) throws IOException {
    AutoscaleSettings mockAutoScaleSettings = Mockito.mock(AutoscaleSettings.class);
    AutoscaleSetting mockAutoScaleSetting = Mockito.mock(AutoscaleSetting.class);
    PagedList<AutoscaleSetting> pageList = getPageList();
    pageList.add(mockAutoScaleSetting);

    Mockito.when(azure.autoscaleSettings()).thenReturn(mockAutoScaleSettings);
    Mockito.when(mockAutoScaleSettings.listByResourceGroup(resourceGroupName)).thenReturn(pageList);
    Mockito.when(mockAutoScaleSetting.autoscaleEnabled()).thenReturn(true);
    Mockito.when(mockAutoScaleSetting.targetResourceId()).thenReturn(targetResourceId);
    Mockito.when(mockAutoScaleSetting.id()).thenReturn(autoScaleSettingId);
    AutoscaleProfile autoscaleProfile = Mockito.mock(AutoscaleProfile.class);
    Mockito.when(mockAutoScaleSetting.profiles()).thenReturn(new HashMap<String, AutoscaleProfile>() {
      { put("profile1", autoscaleProfile); }
    });
    return mockAutoScaleSettings;
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
