package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppRollbackParameters;
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

public class AzureWebAppRollbackTaskHandlerTest extends WingsBaseTest {
  public static final String SLOT_NAME = "slotName";
  public static final double TRAFFIC_WEIGHT = 20.0;
  public static final String ROLLBACK_COMMAND_NAME = "ROLLBACK";
  public static final String APP_NAME = "appName";

  @Mock private ILogStreamingTaskClient mockLogStreamingTaskClient;
  @Mock private LogCallback mockLogCallback;
  @Mock private AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  @Spy @InjectMocks AzureWebAppRollbackTaskHandler azureWebAppRollbackTaskHandler;

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
    AzureWebAppRollbackParameters rollbackParameters = buildAzureWebAppRollbackParameters();
    AzureConfig azureConfig = buildAzureConfig();
    mockSetupSlot();
    mockRerouteProductionSlotTraffic();

    AzureAppServiceTaskResponse azureAppServiceTaskResponse =
        azureWebAppRollbackTaskHandler.executeTaskInternal(rollbackParameters, azureConfig, mockLogStreamingTaskClient);

    assertThat(azureAppServiceTaskResponse).isNotNull();
  }

  private void mockSetupSlot() {
    doNothing().when(azureAppServiceDeploymentService).setupSlot(any());
  }

  private void mockRerouteProductionSlotTraffic() {
    doNothing()
        .when(azureAppServiceDeploymentService)
        .rerouteProductionSlotTraffic(any(), eq(SLOT_NAME), eq(TRAFFIC_WEIGHT), any());
  }

  private AzureWebAppRollbackParameters buildAzureWebAppRollbackParameters() {
    return AzureWebAppRollbackParameters.builder()
        .appId("appId")
        .accountId("accountId")
        .activityId("activityId")
        .resourceGroupName("resourceGroupName")
        .subscriptionId("subscriptionId")
        .timeoutIntervalInMin(15)
        .commandName(ROLLBACK_COMMAND_NAME)
        .appName(APP_NAME)
        .preDeploymentData(buildPreDeploymentData())
        .build();
  }

  private AzureAppServicePreDeploymentData buildPreDeploymentData() {
    return AzureAppServicePreDeploymentData.builder()
        .trafficWeight(TRAFFIC_WEIGHT)
        .connSettingsToAdd(Collections.emptyMap())
        .appSettingsToAdd(Collections.emptyMap())
        .appName(APP_NAME)
        .slotName(SLOT_NAME)
        .imageNameAndTag("imageNameAndTag")
        .build();
  }

  private AzureConfig buildAzureConfig() {
    return AzureConfig.builder().clientId("clientId").key("key".toCharArray()).tenantId("tenantId").build();
  }
}
