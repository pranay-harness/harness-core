package software.wings.service.impl.azure.delegate;

import static io.harness.rule.OwnerRule.IVAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

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
import io.harness.category.element.UnitTests;
import io.harness.network.Http;
import io.harness.rule.Owner;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Azure.class, AzureHelperService.class, Http.class, TimeLimiter.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class AzureAutoScaleSettingsHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private Azure.Configurable configurable;
  @Mock private Azure.Authenticated authenticated;
  @Mock private Azure azure;

  @InjectMocks AzureAutoScaleSettingsHelperServiceDelegateImpl azureAutoScaleSettingsHelperServiceDelegate;

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
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetAutoScaleSettingByTargetResourceId() throws Exception {
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    Optional<AutoscaleSetting> response =
        azureAutoScaleSettingsHelperServiceDelegate.getAutoScaleSettingByTargetResourceId(
            azureConfig, resourceGroupName, targetResourceId);

    response.ifPresent(as -> assertThat(as).isNotNull());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testAttachAutoScaleSettingToTargetResourceId() throws Exception {
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
        + "                \"name\": \"Profile 1\",\n"
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
    AutoscaleSettingResourceInner mockAutoScaleSettingResourceInner = mock(AutoscaleSettingResourceInner.class);
    AutoscaleSettingsInner mockAutoScaleSettingsInner = mock(AutoscaleSettingsInner.class);

    when(mockAutoScaleSettings.inner()).thenReturn(mockAutoScaleSettingsInner);
    ArgumentCaptor<AutoscaleSettingResourceInner> autoScaleSettingResourceInnerCapture =
        ArgumentCaptor.forClass(AutoscaleSettingResourceInner.class);

    doReturn(mockAutoScaleSettingResourceInner)
        .when(mockAutoScaleSettingsInner)
        .createOrUpdate(eq(resourceGroupName), anyString(), autoScaleSettingResourceInnerCapture.capture());

    azureAutoScaleSettingsHelperServiceDelegate.attachAutoScaleSettingToTargetResourceId(
        azureConfig, resourceGroupName, targetResourceId, autoScaleSettingsJSONs, defaultProfileScalePolicy);

    AutoscaleSettingResourceInner autoScaleSettingResult = autoScaleSettingResourceInnerCapture.getValue();
    assertThat(autoScaleSettingResult).isNotNull();
    assertThat(autoScaleSettingResult.targetResourceUri()).isEqualTo(targetResourceId);
    assertThat(autoScaleSettingResult.profiles().size()).isEqualTo(3);
    assertThat(autoScaleSettingResult.profiles().get(0).capacity().minimum())
        .isEqualTo(defaultProfileScalePolicy.minimum());
    assertThat(autoScaleSettingResult.profiles().get(0).capacity().defaultProperty())
        .isEqualTo(defaultProfileScalePolicy.defaultProperty());
    assertThat(autoScaleSettingResult.profiles().get(0).capacity().maximum())
        .isEqualTo(defaultProfileScalePolicy.maximum());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testClearAutoScaleSettingOnTargetResourceId() throws Exception {
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    AutoscaleSettings mockAutoScaleSettings =
        mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    ArgumentCaptor<String> autoScaleSettingIdCapture = ArgumentCaptor.forClass(String.class);
    doNothing().when(mockAutoScaleSettings).deleteById(autoScaleSettingIdCapture.capture());

    azureAutoScaleSettingsHelperServiceDelegate.clearAutoScaleSettingOnTargetResourceId(
        azureConfig, resourceGroupName, targetResourceId);

    String autoScaleSettingIdResult = autoScaleSettingIdCapture.getValue();
    assertThat(autoScaleSettingIdResult).isNotNull();
    assertThat(autoScaleSettingIdResult).isEqualTo(autoScaleSettingId);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetDefaultAutoScaleProfile() throws Exception {
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    Optional<AutoscaleProfile> response = azureAutoScaleSettingsHelperServiceDelegate.getDefaultAutoScaleProfile(
        azureConfig, resourceGroupName, targetResourceId);

    response.ifPresent(as -> assertThat(as).isNotNull());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListAutoScaleProfilesByTargetResourceId() throws Exception {
    String resourceGroupName = "resourceGroupName";
    String targetResourceId = "targetResourceId";
    String autoScaleSettingId = "autoScaleSettingId";
    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").tenantId("tenantId").key("key".toCharArray()).build();

    mockAutosScaleSettings(resourceGroupName, targetResourceId, autoScaleSettingId);

    List<AutoscaleProfile> response =
        azureAutoScaleSettingsHelperServiceDelegate.listAutoScaleProfilesByTargetResourceId(
            azureConfig, resourceGroupName, targetResourceId);

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(1);
  }

  public AutoscaleSettings mockAutosScaleSettings(
      String resourceGroupName, String targetResourceId, String autoScaleSettingId) throws IOException {
    AutoscaleSettings mockAutoScaleSettings = mock(AutoscaleSettings.class);
    AutoscaleSetting mockAutoScaleSetting = mock(AutoscaleSetting.class);
    PagedList<AutoscaleSetting> pageList = getPageList();
    pageList.add(mockAutoScaleSetting);

    when(azure.autoscaleSettings()).thenReturn(mockAutoScaleSettings);
    when(mockAutoScaleSettings.listByResourceGroup(resourceGroupName)).thenReturn(pageList);
    when(mockAutoScaleSetting.autoscaleEnabled()).thenReturn(true);
    when(mockAutoScaleSetting.targetResourceId()).thenReturn(targetResourceId);
    when(mockAutoScaleSetting.id()).thenReturn(autoScaleSettingId);
    AutoscaleProfile autoscaleProfile = mock(AutoscaleProfile.class);
    when(mockAutoScaleSetting.profiles()).thenReturn(new HashMap<String, AutoscaleProfile>() {
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
