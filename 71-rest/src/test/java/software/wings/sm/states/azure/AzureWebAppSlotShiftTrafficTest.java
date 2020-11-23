package software.wings.sm.states.azure;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotResizeResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotShiftTrafficExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceStateData;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotShiftTraffic;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;

public class AzureWebAppSlotShiftTrafficTest extends WingsBaseTest {
  @Mock protected transient DelegateService delegateService;
  @Mock protected transient AzureVMSSStateHelper azureVMSSStateHelper;
  @Mock protected ActivityService activityService;
  @Spy @InjectMocks AzureWebAppSlotShiftTraffic state = new AzureWebAppSlotShiftTraffic("Slot Traffic shift state");

  private final String ACTIVITY_ID = "activityId";

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotTrafficShiftExecuteSuccess() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true);
    ExecutionResponse result = state.execute(mockContext);
    assertSuccessExecution(result);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotTrafficShiftExecuteFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(false, true);
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotTrafficShiftAbsenceOfContextElement() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, false);

    state.setRollback(true);
    ExecutionResponse skipResponse = state.execute(mockContext);
    assertThat(skipResponse.getExecutionStatus()).isEqualTo(SKIPPED);

    state.setRollback(false);
    ExecutionResponse failedResponse = state.execute(mockContext);
    assertThat(failedResponse.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotTrafficShiftHandleAsyncResponse() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true);
    AzureTaskExecutionResponse delegateExecutionResponse = initializeDelegateResponse(true);
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateExecutionResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotTrafficShiftAsyncResponseFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true);
    AzureTaskExecutionResponse delegateExecutionResponse = initializeDelegateResponse(false);
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateExecutionResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  private ExecutionContextImpl initializeMockSetup(boolean isSuccess, boolean contextElement) {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String delegateResult = "Done";

    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    AzureAppServicePreDeploymentData preDeploymentData = AzureAppServicePreDeploymentData.builder().build();
    AzureAppServiceSlotSetupContextElement trafficShiftContextElement = AzureAppServiceSlotSetupContextElement.builder()
                                                                            .preDeploymentData(preDeploymentData)
                                                                            .appServiceSlotSetupTimeOut(10)
                                                                            .build();

    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                            .resourceGroup("resourceGroup")
                                                                            .subscriptionId("subId")
                                                                            .webApp("app-service")
                                                                            .deploymentSlot("stage")
                                                                            .build();

    AzureAppServiceStateData appServiceStateData = AzureAppServiceStateData.builder()
                                                       .application(app)
                                                       .environment(env)
                                                       .service(service)
                                                       .infrastructureMapping(azureWebAppInfrastructureMapping)
                                                       .deploymentSlot("stage")
                                                       .resourceGroup("rg")
                                                       .subscriptionId("subId")
                                                       .azureConfig(azureConfig)
                                                       .artifact(artifact)
                                                       .azureEncryptedDataDetails(encryptedDataDetails)
                                                       .appService("app-service")
                                                       .build();

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);

    if (contextElement) {
      doReturn(trafficShiftContextElement)
          .when(mockContext)
          .getContextElement(eq(ContextElementType.AZURE_WEBAPP_SETUP));
    }

    AzureAppServiceSlotShiftTrafficExecutionData stateExecutionData =
        AzureAppServiceSlotShiftTrafficExecutionData.builder().build();
    doReturn(stateExecutionData).when(mockContext).getStateExecutionData();

    doReturn(appServiceStateData).when(azureVMSSStateHelper).populateAzureAppServiceData(eq(mockContext));
    doReturn(delegateResult).when(delegateService).queueTask(any());

    when(mockContext.renderExpression(anyString())).thenAnswer((Answer<String>) invocation -> {
      Object[] args = invocation.getArguments();
      return (String) args[0];
    });
    if (!isSuccess) {
      doThrow(Exception.class).when(delegateService).queueTask(any());
    }
    state.setTrafficWeightExpr("20");
    return mockContext;
  }

  private AzureTaskExecutionResponse initializeDelegateResponse(boolean isSuccess) {
    AzureWebAppSlotResizeResponse appSlotResizeResponse =
        AzureWebAppSlotResizeResponse.builder()
            .preDeploymentData(AzureAppServicePreDeploymentData.builder().build())
            .build();
    AzureTaskExecutionResponse taskExecutionResponse =
        AzureTaskExecutionResponse.builder()
            .delegateMetaInfo(DelegateMetaInfo.builder().build())
            .commandExecutionStatus(isSuccess ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE)
            .azureTaskResponse(appSlotResizeResponse)
            .build();
    doReturn(isSuccess ? SUCCESS : FAILED)
        .when(azureVMSSStateHelper)
        .getAppServiceExecutionStatus(eq(taskExecutionResponse));
    return taskExecutionResponse;
  }

  private void assertSuccessExecution(ExecutionResponse result) {
    assertThat(result).isNotNull();
    assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getStateExecutionData()).isNotNull();
    assertThat(result.getStateExecutionData()).isInstanceOf(AzureAppServiceSlotShiftTrafficExecutionData.class);
    assertThat(((AzureAppServiceSlotShiftTrafficExecutionData) result.getStateExecutionData()).getActivityId())
        .isEqualTo(ACTIVITY_ID);
  }
}
