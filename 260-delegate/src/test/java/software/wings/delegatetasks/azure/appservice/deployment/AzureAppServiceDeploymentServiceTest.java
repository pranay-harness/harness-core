/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.IMAGE_AND_TAG_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SAVE_EXISTING_CONFIGURATIONS;
import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_STARTING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.WEB_APP_INSTANCE_STATUS_RUNNING;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;
import io.harness.logstreaming.LogStreamingTaskClient;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.implementation.SiteInstanceInner;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import rx.Completable;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServiceDeploymentServiceTest extends WingsBaseTest {
  private static final String SLOT_NAME = "slotName";
  private static final String TARGET_SLOT_NAME = "targetSlotName";
  private static final String APP_NAME = "appName";
  private static final String RESOURCE_GROUP_NAME = "resourceGroupName";
  private static final String SUBSCRIPTION_ID = "subscriptionId";
  private static final String IMAGE_AND_TAG = "image/tag";
  private static final String AZURE_REGISTRY_NAME = "azureRegistryName";
  private static final String DEPLOYMENT_SLOT_ID = "deploymentSlotId";
  private static final String APP_SERVICE_PLAN_ID = "appServicePlanId";
  private static final String DEFAULT_HOST_NAME = "defaultHostName";

  @Mock private AzureWebClient mockAzureWebClient;
  @Mock private AzureContainerRegistryClient mockAzureContainerRegistryClient;
  @Mock private AzureTimeLimiter mockAzureTimeLimiter;
  @Mock private AzureMonitorClient mockAzureMonitorClient;
  @Mock private LogStreamingTaskClient mockLogStreamingTaskClient;
  @Mock private LogCallback mockLogCallback;
  @Mock private SlotSteadyStateChecker slotSteadyStateChecker;

  @Spy @InjectMocks AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  @Before
  public void setup() {
    doReturn(mockLogCallback).when(mockLogStreamingTaskClient).obtainLogCallback(anyString());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any(), any());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeployDockerImage() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove =
        Collections.singletonMap("appSetting2", getAppSettings("appSetting2"));
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd =
        Collections.singletonMap("appSetting1", getAppSettings("appSetting1"));
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd =
        Collections.singletonMap("appSetting1", getConnectionSettings("connSetting1"));
    Map<String, AzureAppServiceConnectionString> connSettingsToRemove =
        Collections.singletonMap("appSetting1", getConnectionSettings("connSetting1"));
    Map<String, AzureAppServiceApplicationSetting> dockerSettings = Collections.singletonMap("dockerSetting1",
        AzureAppServiceApplicationSetting.builder().name("dockerSetting1").value("dockerSetting1value").build());

    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(dockerSettings)
            .azureWebClientContext(azureWebClientContext)
            .appSettingsToAdd(appSettingsToAdd)
            .appSettingsToRemove(appSettingsToRemove)
            .connSettingsToAdd(connSettingsToAdd)
            .connSettingsToRemove(connSettingsToRemove)
            .build();

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Completable.complete()).when(deploymentSlot).stopAsync();
    doReturn(Completable.complete()).when(deploymentSlot).startAsync();
    doReturn(Optional.of(deploymentSlot))
        .when(mockAzureWebClient)
        .getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    azureAppServiceDeploymentService.deployDockerImage(
        azureAppServiceDockerDeploymentContext, AzureAppServicePreDeploymentData.builder().build());

    verify(slotSteadyStateChecker, times(1))
        .waitUntilCompleteWithTimeout(
            eq(1L), eq(SLOT_STOPPING_STATUS_CHECK_INTERVAL), any(), eq(STOP_DEPLOYMENT_SLOT), any());
    verify(slotSteadyStateChecker, times(1))
        .waitUntilCompleteWithTimeout(
            eq(1L), eq(SLOT_STARTING_STATUS_CHECK_INTERVAL), any(), eq(START_DEPLOYMENT_SLOT), any());
    verify(mockAzureWebClient, times(1))
        .deleteDeploymentSlotAppSettings(azureWebClientContext, SLOT_NAME, appSettingsToRemove);
    verify(mockAzureWebClient, times(1))
        .updateDeploymentSlotAppSettings(azureWebClientContext, SLOT_NAME, appSettingsToAdd);
    verify(mockAzureWebClient, times(1))
        .deleteDeploymentSlotConnectionStrings(azureWebClientContext, SLOT_NAME, connSettingsToRemove);
    verify(mockAzureWebClient, times(1))
        .updateDeploymentSlotConnectionStrings(azureWebClientContext, SLOT_NAME, connSettingsToAdd);
    verify(mockAzureWebClient, times(1))
        .updateDeploymentSlotDockerSettings(azureWebClientContext, SLOT_NAME, dockerSettings);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testDeployDockerImageFailure() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove =
        Collections.singletonMap("appSetting2", getAppSettings("appSetting2"));
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd =
        Collections.singletonMap("appSetting1", getAppSettings("appSetting1"));
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd =
        Collections.singletonMap("appSetting1", getConnectionSettings("connSetting1"));
    Map<String, AzureAppServiceConnectionString> connSettingsToRemove =
        Collections.singletonMap("appSetting1", getConnectionSettings("connSetting1"));
    Map<String, AzureAppServiceApplicationSetting> dockerSettings = Collections.singletonMap("dockerSetting1",
        AzureAppServiceApplicationSetting.builder().name("dockerSetting1").value("dockerSetting1value").build());

    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(dockerSettings)
            .azureWebClientContext(azureWebClientContext)
            .appSettingsToAdd(appSettingsToAdd)
            .appSettingsToRemove(appSettingsToRemove)
            .connSettingsToAdd(connSettingsToAdd)
            .connSettingsToRemove(connSettingsToRemove)
            .build();

    doThrow(Exception.class).when(mockAzureWebClient).deleteDeploymentSlotAppSettings(any(), any(), any());
    assertThatThrownBy(()
                           -> azureAppServiceDeploymentService.deployDockerImage(azureAppServiceDockerDeploymentContext,
                               AzureAppServicePreDeploymentData.builder().build()))
        .isInstanceOf(Exception.class);

    reset(mockAzureWebClient);
    doThrow(Exception.class).when(mockAzureWebClient).stopDeploymentSlotAsync(any(), any(), any());
    assertThatThrownBy(()
                           -> azureAppServiceDeploymentService.deployDockerImage(azureAppServiceDockerDeploymentContext,
                               AzureAppServicePreDeploymentData.builder().build()))
        .isInstanceOf(Exception.class);

    reset(mockAzureWebClient);
    doThrow(Exception.class).when(mockAzureWebClient).startDeploymentSlotAsync(any(), any(), any());
    assertThatThrownBy(()
                           -> azureAppServiceDeploymentService.deployDockerImage(azureAppServiceDockerDeploymentContext,
                               AzureAppServicePreDeploymentData.builder().build()))
        .isInstanceOf(Exception.class);

    reset(mockAzureWebClient);
    doThrow(Exception.class).when(mockAzureWebClient).deleteDeploymentSlotDockerSettings(any(), any());
    assertThatThrownBy(()
                           -> azureAppServiceDeploymentService.deployDockerImage(azureAppServiceDockerDeploymentContext,
                               AzureAppServicePreDeploymentData.builder().build()))
        .isInstanceOf(Exception.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeployDockerImageNoAppAndConnSettingsToAddOrRemove() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(new HashMap<>())
            .azureWebClientContext(azureWebClientContext)
            .build();

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Completable.complete()).when(deploymentSlot).stopAsync();
    doReturn(Completable.complete()).when(deploymentSlot).startAsync();
    doReturn(Optional.of(deploymentSlot))
        .when(mockAzureWebClient)
        .getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    azureAppServiceDeploymentService.deployDockerImage(
        azureAppServiceDockerDeploymentContext, AzureAppServicePreDeploymentData.builder().build());

    verify(slotSteadyStateChecker, times(1))
        .waitUntilCompleteWithTimeout(
            eq(1L), eq(SLOT_STOPPING_STATUS_CHECK_INTERVAL), any(), eq(STOP_DEPLOYMENT_SLOT), any());
    verify(slotSteadyStateChecker, times(1))
        .waitUntilCompleteWithTimeout(
            eq(1L), eq(SLOT_STARTING_STATUS_CHECK_INTERVAL), any(), eq(START_DEPLOYMENT_SLOT), any());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeployDockerImageInvalidImagePathAndTag() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag("")
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(new HashMap<>())
            .azureWebClientContext(azureWebClientContext)
            .build();

    doReturn(Optional.empty()).when(mockAzureWebClient).getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureAppServiceDeploymentService.deployDockerImage(
                            azureAppServiceDockerDeploymentContext, AzureAppServicePreDeploymentData.builder().build()))
        .withMessageContaining(IMAGE_AND_TAG_BLANK_ERROR_MSG);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeployDockerImageInvalidSlotName() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName("")
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(new HashMap<>())
            .azureWebClientContext(azureWebClientContext)
            .build();

    doReturn(Optional.empty()).when(mockAzureWebClient).getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureAppServiceDeploymentService.deployDockerImage(
                            azureAppServiceDockerDeploymentContext, AzureAppServicePreDeploymentData.builder().build()))
        .withMessageContaining(SLOT_NAME_BLANK_ERROR_MSG);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeployDockerImageInvalidAppName() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    azureWebClientContext.setAppName("");
    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(new HashMap<>())
            .azureWebClientContext(azureWebClientContext)
            .build();

    doReturn(Optional.empty()).when(mockAzureWebClient).getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureAppServiceDeploymentService.deployDockerImage(
                            azureAppServiceDockerDeploymentContext, AzureAppServicePreDeploymentData.builder().build()))
        .withMessageContaining(WEB_APP_NAME_BLANK_ERROR_MSG);

    assertThat(azureAppServiceDockerDeploymentContext.toString()).isNotNull();
    assertThat(azureAppServiceDockerDeploymentContext.equals(AzureAppServiceDockerDeploymentContext.builder().build()))
        .isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testFetchDeploymentData() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(DEPLOYMENT_SLOT_ID).when(deploymentSlot).id();
    doReturn(APP_SERVICE_PLAN_ID).when(deploymentSlot).appServicePlanId();
    doReturn(DEFAULT_HOST_NAME).when(deploymentSlot).defaultHostName();

    doReturn(Optional.of(deploymentSlot))
        .when(mockAzureWebClient)
        .getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    SiteInstanceInner siteInstanceInner = mock(SiteInstanceInner.class);
    doReturn("id").when(siteInstanceInner).id();
    doReturn("name").when(siteInstanceInner).name();
    doReturn("type").when(siteInstanceInner).type();

    doReturn(Collections.singletonList(siteInstanceInner))
        .when(mockAzureWebClient)
        .listInstanceIdentifiersSlot(azureWebClientContext, SLOT_NAME);

    List<AzureAppDeploymentData> azureAppDeploymentDataList =
        azureAppServiceDeploymentService.fetchDeploymentData(azureWebClientContext, SLOT_NAME);

    Assertions.assertThat(azureAppDeploymentDataList.size()).isEqualTo(1);
    AzureAppDeploymentData appDeploymentData = azureAppDeploymentDataList.get(0);
    Assertions.assertThat(appDeploymentData.getInstanceState()).isEqualTo(WEB_APP_INSTANCE_STATUS_RUNNING);
    Assertions.assertThat(appDeploymentData.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    Assertions.assertThat(appDeploymentData.getResourceGroup()).isEqualTo(RESOURCE_GROUP_NAME);
    Assertions.assertThat(appDeploymentData.getAppName()).isEqualTo(APP_NAME);
    Assertions.assertThat(appDeploymentData.getDeploySlot()).isEqualTo(SLOT_NAME);
    Assertions.assertThat(appDeploymentData.getDeploySlotId()).isEqualTo(DEPLOYMENT_SLOT_ID);
    Assertions.assertThat(appDeploymentData.getAppServicePlanId()).isEqualTo(APP_SERVICE_PLAN_ID);
    Assertions.assertThat(appDeploymentData.getHostName()).isEqualTo(DEFAULT_HOST_NAME);
    Assertions.assertThat(appDeploymentData.getInstanceName()).isEqualTo("name");
    Assertions.assertThat(appDeploymentData.getInstanceId()).isEqualTo("id");
    Assertions.assertThat(appDeploymentData.getInstanceType()).isEqualTo("type");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testFetchDeploymentDataNoDeploymentSlot() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    doReturn(Optional.empty()).when(mockAzureWebClient).getDeploymentSlotByName(azureWebClientContext, SLOT_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> azureAppServiceDeploymentService.fetchDeploymentData(azureWebClientContext, SLOT_NAME));
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetAzureAppServicePreDeploymentData() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings = new HashMap<>();
    userAddedAppSettings.put("appSetting1", getAppSettings("appSetting1"));
    userAddedAppSettings.put("appSetting2", getAppSettings("appSetting2"));

    Map<String, AzureAppServiceConnectionString> userAddedConnSettings = new HashMap<>();

    userAddedConnSettings.put("connSetting1", getConnectionSettings("connSetting1"));
    userAddedConnSettings.put("connSetting2", getConnectionSettings("connSetting2"));

    Map<String, AzureAppServiceApplicationSetting> existingAppSettingsOnSlot = new HashMap<>();
    existingAppSettingsOnSlot.put("appSetting1", getAppSettings("appSetting1"));

    doReturn(existingAppSettingsOnSlot)
        .when(mockAzureWebClient)
        .listDeploymentSlotAppSettings(azureWebClientContext, SLOT_NAME);

    Map<String, AzureAppServiceConnectionString> existingConnSettingsOnSlot = new HashMap<>();
    existingConnSettingsOnSlot.put("connSetting1", getConnectionSettings("connSetting1"));

    doReturn(existingConnSettingsOnSlot)
        .when(mockAzureWebClient)
        .listDeploymentSlotConnectionStrings(azureWebClientContext, SLOT_NAME);

    Map<String, AzureAppServiceApplicationSetting> dockerSettingsNeedBeUpdatedInRollback = new HashMap<>();
    doReturn(dockerSettingsNeedBeUpdatedInRollback)
        .when(mockAzureWebClient)
        .listDeploymentSlotDockerSettings(azureWebClientContext, SLOT_NAME);

    Optional<String> dockerImageNameAndTag = Optional.of(IMAGE_AND_TAG);
    doReturn(dockerImageNameAndTag)
        .when(mockAzureWebClient)
        .getSlotDockerImageNameAndTag(azureWebClientContext, SLOT_NAME);

    doReturn(2.0).when(mockAzureWebClient).getDeploymentSlotTrafficWeight(azureWebClientContext, SLOT_NAME);

    ILogStreamingTaskClient logStreamingTaskClient = mock(ILogStreamingTaskClient.class);
    LogCallback logCallback = mock(LogCallback.class);
    doReturn(logCallback).when(logStreamingTaskClient).obtainLogCallback(SAVE_EXISTING_CONFIGURATIONS);

    AzureAppServicePreDeploymentData azureAppServicePreDeploymentData =
        azureAppServiceDeploymentService.getAzureAppServicePreDeploymentData(azureWebClientContext, SLOT_NAME,
            TARGET_SLOT_NAME, userAddedAppSettings, userAddedConnSettings, logStreamingTaskClient);

    Assertions.assertThat(azureAppServicePreDeploymentData.getAppSettingsToRemove().get("appSetting2")).isNotNull();
    Assertions.assertThat(azureAppServicePreDeploymentData.getAppSettingsToAdd().get("appSetting1")).isNotNull();
    Assertions.assertThat(azureAppServicePreDeploymentData.getConnStringsToRemove().get("connSetting2")).isNotNull();
    Assertions.assertThat(azureAppServicePreDeploymentData.getConnStringsToAdd().get("connSetting1")).isNotNull();
    Assertions.assertThat(azureAppServicePreDeploymentData.getAppSettingsToRemove().size()).isEqualTo(1);
    Assertions.assertThat(azureAppServicePreDeploymentData.getAppSettingsToAdd().size()).isEqualTo(1);
    Assertions.assertThat(azureAppServicePreDeploymentData.getConnStringsToRemove().size()).isEqualTo(1);
    Assertions.assertThat(azureAppServicePreDeploymentData.getConnStringsToAdd().size()).isEqualTo(1);
    Assertions.assertThat(azureAppServicePreDeploymentData.getDockerSettingsToAdd().size()).isEqualTo(0);
    Assertions.assertThat(azureAppServicePreDeploymentData.getAppName()).isEqualTo(APP_NAME);
    Assertions.assertThat(azureAppServicePreDeploymentData.getImageNameAndTag()).isEqualTo(IMAGE_AND_TAG);
    Assertions.assertThat(azureAppServicePreDeploymentData.getTrafficWeight()).isEqualTo(2);

    assertThatThrownBy(
        ()
            -> azureAppServiceDeploymentService.getAzureAppServicePreDeploymentData(azureWebClientContext, "",
                TARGET_SLOT_NAME, userAddedAppSettings, userAddedConnSettings, logStreamingTaskClient))
        .isInstanceOf(Exception.class);

    doReturn("STOPPED").when(mockAzureWebClient).getSlotState(any(), eq(TARGET_SLOT_NAME));
    assertThatThrownBy(
        ()
            -> azureAppServiceDeploymentService.getAzureAppServicePreDeploymentData(azureWebClientContext, SLOT_NAME,
                TARGET_SLOT_NAME, userAddedAppSettings, userAddedConnSettings, logStreamingTaskClient))
        .isInstanceOf(Exception.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetContainerRegistryCredentials() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName("")
                                                                                .build();
    RegistryCredentials registryCredentials = mock(RegistryCredentials.class);
    ArgumentCaptor<AzureContainerRegistryClientContext> argumentCaptor =
        ArgumentCaptor.forClass(AzureContainerRegistryClientContext.class);
    doReturn(Optional.of(registryCredentials))
        .when(mockAzureContainerRegistryClient)
        .getContainerRegistryCredentials(argumentCaptor.capture());
    Registry registry = mock(Registry.class);
    doReturn(RESOURCE_GROUP_NAME).when(registry).resourceGroupName();
    doReturn(Optional.of(registry))
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    RegistryCredentials resultRegistryCredentials = azureAppServiceDeploymentService.getContainerRegistryCredentials(
        azureConfig, azureContainerRegistryConnectorDTO);

    Assertions.assertThat(resultRegistryCredentials).isEqualTo(registryCredentials);
    AzureContainerRegistryClientContext azureContainerRegistryClientContext = argumentCaptor.getValue();
    Assertions.assertThat(azureContainerRegistryClientContext.getResourceGroupName()).isEqualTo(RESOURCE_GROUP_NAME);
    Assertions.assertThat(azureContainerRegistryClientContext.getRegistryName()).isEqualTo(AZURE_REGISTRY_NAME);
    Assertions.assertThat(azureContainerRegistryClientContext.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    Assertions.assertThat(azureContainerRegistryClientContext.getAzureConfig()).isEqualTo(azureConfig);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNoContainerRegistryCredentials() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName(RESOURCE_GROUP_NAME)
                                                                                .build();
    doReturn(Optional.empty()).when(mockAzureContainerRegistryClient).getContainerRegistryCredentials(any());
    doReturn(Optional.of(Registry.class))
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureAppServiceDeploymentService.getContainerRegistryCredentials(
                            azureConfig, azureContainerRegistryConnectorDTO))
        .withMessageContaining("Not found container registry credentials");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNoFirstContainerRegistryByName() {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureContainerRegistryConnectorDTO azureContainerRegistryConnectorDTO = AzureContainerRegistryConnectorDTO.builder()
                                                                                .subscriptionId(SUBSCRIPTION_ID)
                                                                                .azureRegistryName(AZURE_REGISTRY_NAME)
                                                                                .resourceGroupName("")
                                                                                .build();

    doReturn(Optional.empty())
        .when(mockAzureContainerRegistryClient)
        .findFirstContainerRegistryByNameOnSubscription(azureConfig, SUBSCRIPTION_ID, AZURE_REGISTRY_NAME);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> azureAppServiceDeploymentService.getContainerRegistryCredentials(
                            azureConfig, azureContainerRegistryConnectorDTO))
        .withMessageContaining("Not found Azure container registry by name");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testRerouteProductionSlotTraffic() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    azureAppServiceDeploymentService.rerouteProductionSlotTraffic(
        azureWebClientContext, SLOT_NAME, 50, mockLogStreamingTaskClient);
    verify(mockAzureWebClient, times(1)).rerouteProductionSlotTraffic(azureWebClientContext, SLOT_NAME, 50);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void swapSlotsUsingCallback() {
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    AzureAppServiceDockerDeploymentContext azureAppServiceDockerDeploymentContext =
        AzureAppServiceDockerDeploymentContext.builder()
            .imagePathAndTag(IMAGE_AND_TAG)
            .slotName(SLOT_NAME)
            .steadyStateTimeoutInMin(1)
            .logStreamingTaskClient(mockLogStreamingTaskClient)
            .dockerSettings(new HashMap<>())
            .azureWebClientContext(azureWebClientContext)
            .build();

    azureAppServiceDeploymentService.swapSlotsUsingCallback(
        azureAppServiceDockerDeploymentContext, TARGET_SLOT_NAME, mockLogStreamingTaskClient);

    ArgumentCaptor<SlotStatusVerifier> statusVerifierArgument = ArgumentCaptor.forClass(SlotStatusVerifier.class);

    verify(slotSteadyStateChecker, times(1))
        .waitUntilCompleteWithTimeout(
            eq(1L), eq(SLOT_STOPPING_STATUS_CHECK_INTERVAL), any(), eq(SLOT_SWAP), statusVerifierArgument.capture());
    SlotStatusVerifier statusVerifier = statusVerifierArgument.getValue();
    assertThat(statusVerifier).isNotNull();
    assertThat(statusVerifier).isInstanceOf(SwapSlotStatusVerifier.class);
  }

  private AzureWebClientContext getAzureWebClientContext() {
    return AzureWebClientContext.builder()
        .appName(APP_NAME)
        .resourceGroupName(RESOURCE_GROUP_NAME)
        .subscriptionId(SUBSCRIPTION_ID)
        .azureConfig(AzureConfig.builder().build())
        .build();
  }

  private AzureAppServiceConnectionString getConnectionSettings(String name) {
    return AzureAppServiceConnectionString.builder().name(name).value(name + "value").build();
  }

  private AzureAppServiceApplicationSetting getAppSettings(String name) {
    return AzureAppServiceApplicationSetting.builder().name(name).value(name + "value").build();
  }
}
