package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentService;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureWebAppSlotSetupTaskHandlerTest extends WingsBaseTest {
  @Mock private ILogStreamingTaskClient mockLogStreamingTaskClient;
  @Mock private LogCallback mockLogCallback;
  @Mock private AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  @Spy @InjectMocks AzureWebAppSlotSetupTaskHandler azureWebAppSlotSetupTaskHandler;

  @Before
  public void setup() {
    doReturn(mockLogCallback).when(mockLogStreamingTaskClient).obtainLogCallback(anyString());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any(), any());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString(), any());
    doNothing().when(mockLogCallback).saveExecutionLog(anyString());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternal() {
    AzureConfig azureConfig = buildAzureConfig();
    AzureAppServiceTaskParameters setupParameters = buildAzureAppServiceTaskParameters();
    AzureAppServicePreDeploymentData appServicePreDeploymentData = buildAzureAppServicePreDeploymentData();
    AzureAppDeploymentData azureAppDeploymentData = buildAzureAppDeploymentData();

    doNothing().when(azureAppServiceDeploymentService).deployDockerImage(any());

    doReturn(appServicePreDeploymentData)
        .when(azureAppServiceDeploymentService)
        .getAzureAppServicePreDeploymentData(any(), anyString(), any(), any());

    doReturn(Collections.singletonList(azureAppDeploymentData))
        .when(azureAppServiceDeploymentService)
        .fetchDeploymentData(any(), anyString());

    AzureAppServiceTaskResponse azureAppServiceTaskResponse =
        azureWebAppSlotSetupTaskHandler.executeTaskInternal(setupParameters, azureConfig, mockLogStreamingTaskClient);

    assertThat(azureAppServiceTaskResponse).isNotNull();
    assertThat(azureAppServiceTaskResponse).isInstanceOf(AzureWebAppSlotSetupResponse.class);
    AzureWebAppSlotSetupResponse slotSetupResponse = (AzureWebAppSlotSetupResponse) azureAppServiceTaskResponse;

    assertThat(slotSetupResponse.getErrorMsg()).isNull();
    assertThat(slotSetupResponse.getAzureAppDeploymentData()).isNotNull();
    assertThat(slotSetupResponse.getAzureAppDeploymentData().size()).isEqualTo(1);
    assertThat(slotSetupResponse.getAzureAppDeploymentData().get(0))
        .isEqualToComparingFieldByField(azureAppDeploymentData);

    assertThat(slotSetupResponse.getPreDeploymentData()).isNotNull();
    assertThat(slotSetupResponse.getPreDeploymentData()).isEqualToComparingFieldByField(appServicePreDeploymentData);
  }

  private AzureAppServicePreDeploymentData buildAzureAppServicePreDeploymentData() {
    return AzureAppServicePreDeploymentData.builder()
        .appName("preAppName")
        .dockerSettingsToAdd(Collections.emptyMap())
        .connSettingsToRemove(Collections.emptyMap())
        .appSettingsToRemove(Collections.emptyMap())
        .appSettingsToAdd(Collections.emptyMap())
        .connSettingsToAdd(Collections.emptyMap())
        .slotName("preSlotName")
        .imageNameAndTag("preImageNameAndTag")
        .build();
  }

  private AzureAppDeploymentData buildAzureAppDeploymentData() {
    return AzureAppDeploymentData.builder()
        .subscriptionId("subscriptionId")
        .resourceGroup("resourceGroup")
        .instanceState("running")
        .instanceType("Microsoft.Compute/webApp")
        .instanceName("instanceName")
        .appName("appName")
        .deploySlot("deploySlotName")
        .deploySlotId("deploySlotId")
        .hostName("hostName")
        .instanceIp("instanceIp")
        .build();
  }

  private AzureAppServiceTaskParameters buildAzureAppServiceTaskParameters() {
    return AzureWebAppSlotSetupParameters.builder()
        .azureRegistryType(AzureRegistryType.ARTIFACTORY_PRIVATE_REGISTRY)
        .encryptedDataDetails(Collections.emptyList())
        .connectorConfigDTO(buildArtifactoryServerUr())
        .imageTag("imageTag")
        .imageName("imageName")
        .accountId("accountId")
        .webAppName("webAppName")
        .appSettings(Collections.emptyMap())
        .connSettings(Collections.emptyMap())
        .resourceGroupName("resourceGroupName")
        .slotName("slotName")
        .subscriptionId("subscriptionId")
        .activityId("activityId")
        .timeoutIntervalInMin(15)
        .build();
  }

  private ArtifactoryConnectorDTO buildArtifactoryServerUr() {
    return ArtifactoryConnectorDTO.builder()
        .artifactoryServerUrl("artifactoryServerUr")
        .auth(buildArtifactoryAuthenticationDTO())
        .build();
  }

  private ArtifactoryAuthenticationDTO buildArtifactoryAuthenticationDTO() {
    return ArtifactoryAuthenticationDTO.builder()
        .authType(ArtifactoryAuthType.USER_PASSWORD)
        .credentials(buildArtifactoryUsernamePasswordAuthDTO())
        .build();
  }

  private ArtifactoryUsernamePasswordAuthDTO buildArtifactoryUsernamePasswordAuthDTO() {
    return ArtifactoryUsernamePasswordAuthDTO.builder()
        .username("username")
        .passwordRef(new SecretRefData("", Scope.ACCOUNT, "decryptedValue".toCharArray()))
        .build();
  }

  private AzureConfig buildAzureConfig() {
    return AzureConfig.builder().clientId("clientId").key("key".toCharArray()).tenantId("tenantId").build();
  }
}
