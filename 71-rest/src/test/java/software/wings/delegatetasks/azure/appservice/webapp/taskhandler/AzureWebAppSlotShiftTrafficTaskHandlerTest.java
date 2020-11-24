package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_WEIGHT;
import static io.harness.azure.model.AzureConstants.TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.rule.OwnerRule.IVAN;

import static com.cronutils.utils.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotShiftTrafficParameters;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AzureWebAppSlotShiftTrafficTaskHandlerTest extends WingsBaseTest {
  public static final double TRAFFIC_WEIGHT_IN_PERCENTAGE = 50.0;
  public static final String SHIFT_TRAFFIC_SLOT_NAME = "slotName";
  public static final int INVALID_TRAFFIC_WEIGHT_IN_PERCENTAGE = -50;

  @Mock private AzureWebClient mockAzureWebClient;
  @Mock private ILogStreamingTaskClient mockLogStreamingTaskClient;
  @Mock private LogCallback mockLogCallback;

  @Spy @InjectMocks AzureWebAppSlotShiftTrafficTaskHandler slotShiftTrafficTaskHandler;

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
    AzureWebAppSlotShiftTrafficParameters azureAppServiceTaskParameters = buildAzureWebAppSlotShiftTrafficParameters();
    AzureConfig azureConfig = buildAzureConfig();
    mockRerouteProductionSlotTraffic();

    AzureAppServiceTaskResponse azureAppServiceTaskResponse = slotShiftTrafficTaskHandler.executeTaskInternal(
        azureAppServiceTaskParameters, azureConfig, mockLogStreamingTaskClient);

    assertThat(azureAppServiceTaskResponse).isNotNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithBlankWebAppNameTaskParam() {
    AzureWebAppSlotShiftTrafficParameters azureAppServiceTaskParameters = buildAzureWebAppSlotShiftTrafficParameters();
    AzureConfig azureConfig = buildAzureConfig();

    azureAppServiceTaskParameters.setAppName(EMPTY);

    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> slotShiftTrafficTaskHandler.executeTaskInternal(
                            azureAppServiceTaskParameters, azureConfig, mockLogStreamingTaskClient))
        .withMessageContaining(WEB_APP_NAME_BLANK_ERROR_MSG);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithBlankSlotNameTaskParam() {
    AzureWebAppSlotShiftTrafficParameters azureAppServiceTaskParameters = buildAzureWebAppSlotShiftTrafficParameters();
    AzureConfig azureConfig = buildAzureConfig();

    azureAppServiceTaskParameters.setShiftTrafficSlotName(EMPTY);

    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> slotShiftTrafficTaskHandler.executeTaskInternal(
                            azureAppServiceTaskParameters, azureConfig, mockLogStreamingTaskClient))
        .withMessageContaining(SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithInvalidTrafficWeightTaskParam() {
    AzureWebAppSlotShiftTrafficParameters azureAppServiceTaskParameters = buildAzureWebAppSlotShiftTrafficParameters();
    AzureConfig azureConfig = buildAzureConfig();

    azureAppServiceTaskParameters.setTrafficWeightInPercentage(INVALID_TRAFFIC_WEIGHT_IN_PERCENTAGE);

    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> slotShiftTrafficTaskHandler.executeTaskInternal(
                            azureAppServiceTaskParameters, azureConfig, mockLogStreamingTaskClient))
        .withMessageContaining(TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG);
  }

  private void mockRerouteProductionSlotTraffic() {
    doNothing()
        .when(mockAzureWebClient)
        .rerouteProductionSlotTraffic(any(), eq(SHIFT_TRAFFIC_SLOT_NAME), eq(TRAFFIC_WEIGHT_IN_PERCENTAGE));
  }

  private AzureWebAppSlotShiftTrafficParameters buildAzureWebAppSlotShiftTrafficParameters() {
    return AzureWebAppSlotShiftTrafficParameters.builder()
        .appId("appId")
        .accountId("accountId")
        .activityId("activityId")
        .resourceGroupName("resourceGroupName")
        .subscriptionId("subscriptionId")
        .timeoutIntervalInMin(15)
        .commandName(SLOT_TRAFFIC_WEIGHT)
        .webAppName("webAppName")
        .shiftTrafficSlotName(SHIFT_TRAFFIC_SLOT_NAME)
        .trafficWeightInPercentage(TRAFFIC_WEIGHT_IN_PERCENTAGE)
        .build();
  }

  private AzureConfig buildAzureConfig() {
    return AzureConfig.builder().clientId("clientId").key("key".toCharArray()).tenantId("tenantId").build();
  }
}
