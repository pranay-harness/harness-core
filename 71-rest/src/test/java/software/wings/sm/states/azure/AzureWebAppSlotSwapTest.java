package software.wings.sm.states.azure;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANIL;

import static software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement.AMI_SERVICE_SETUP_SWEEPING_OUTPUT_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSwapSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
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
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSwapExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSwapExecutionSummary;
import software.wings.sm.states.azure.appservices.AzureAppServiceStateData;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotSwap;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;

public class AzureWebAppSlotSwapTest extends WingsBaseTest {
  @Mock protected transient DelegateService delegateService;
  @Mock protected transient AzureVMSSStateHelper azureVMSSStateHelper;
  @Mock protected ActivityService activityService;
  @Mock protected transient AzureSweepingOutputServiceHelper azureSweepingOutputServiceHelper;
  @Spy @InjectMocks AzureWebAppSlotSwap state = new AzureWebAppSlotSwap("Slot swap state");

  private final String ACTIVITY_ID = "activityId";
  private final String SWAP_RESOURCE_GROUP = "swapSlotResourceGroup";
  private final String SWAP_APP_NAME = "swapSlotWebApp";
  private final String SWAP_TARGET_SLOT = "swapTargetSlot";
  private final String SWAP_DEPLOYMENT_SLOT = "swapDeploymentSlot";
  private final String INFRA_MAPPING_ID = "infraMappingId";

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotExecuteSuccess() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true, false);

    assertThat(state.validateFields()).isNotEmpty();
    assertThat(state.validateFields().size()).isEqualTo(4);
    initializeStateFields();
    assertThat(state.validateFields()).isEmpty();

    state.handleAbortEvent(mockContext);
    assertThat(state.skipMessage()).isNotEmpty();
    ExecutionResponse response = state.execute(mockContext);
    assertSuccessExecution(response);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotExecuteFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(false, true, false);
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotExecuteSaveActivityFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(false, true, true);
    state.execute(mockContext);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotExecuteSaveActivityWingsExceptionFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(false, true, true);
    state.execute(mockContext);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotAbsenceOfContextElement() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, false, false);
    ExecutionResponse failedResponse = state.execute(mockContext);
    assertThat(failedResponse.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotHandleAsyncResponse() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true, false);
    AzureTaskExecutionResponse delegateExecutionResponse = initializeDelegateResponse(true, mockContext, false);
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateExecutionResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotAsyncResponseFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true, false);
    AzureTaskExecutionResponse delegateExecutionResponse = initializeDelegateResponse(false, mockContext, false);
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateExecutionResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotAsyncResponseGenericFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true, false);
    AzureTaskExecutionResponse delegateExecutionResponse = initializeDelegateResponse(true, mockContext, true);
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateExecutionResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  private ExecutionContextImpl initializeMockSetup(
      boolean successEnequeueDelegateTask, boolean contextElement, boolean failActivityCreation) {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String delegateResult = "Done";

    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    AzureAppServicePreDeploymentData preDeploymentData = AzureAppServicePreDeploymentData.builder().build();
    AzureAppServiceSlotSetupContextElement setupContextElement = AzureAppServiceSlotSetupContextElement.builder()
                                                                     .preDeploymentData(preDeploymentData)
                                                                     .deploymentSlot(SWAP_DEPLOYMENT_SLOT)
                                                                     .appServiceSlotSetupTimeOut(10)
                                                                     .build();

    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                            .uuid(INFRA_MAPPING_ID)
                                                                            .resourceGroup(SWAP_RESOURCE_GROUP)
                                                                            .subscriptionId("subId")
                                                                            .webApp(SWAP_APP_NAME)
                                                                            .deploymentSlot(SWAP_DEPLOYMENT_SLOT)
                                                                            .build();

    AzureAppServiceStateData appServiceStateData = AzureAppServiceStateData.builder()
                                                       .application(app)
                                                       .environment(env)
                                                       .service(service)
                                                       .infrastructureMapping(azureWebAppInfrastructureMapping)
                                                       .deploymentSlot(SWAP_DEPLOYMENT_SLOT)
                                                       .resourceGroup(SWAP_RESOURCE_GROUP)
                                                       .subscriptionId("subId")
                                                       .azureConfig(azureConfig)
                                                       .artifact(artifact)
                                                       .azureEncryptedDataDetails(encryptedDataDetails)
                                                       .appService(SWAP_APP_NAME)
                                                       .build();

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    if (contextElement) {
      doReturn(setupContextElement).when(mockContext).getContextElement(eq(ContextElementType.AZURE_WEBAPP_SETUP));
      doReturn(setupContextElement)
          .when(azureSweepingOutputServiceHelper)
          .getSetupElementFromSweepingOutput(eq(mockContext), eq(AMI_SERVICE_SETUP_SWEEPING_OUTPUT_NAME));
    }

    if (failActivityCreation) {
      doThrow(Exception.class)
          .when(azureVMSSStateHelper)
          .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    } else {
      doReturn(activity)
          .when(azureVMSSStateHelper)
          .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    }

    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    doReturn(appServiceStateData).when(azureVMSSStateHelper).populateAzureAppServiceData(eq(mockContext));
    doReturn(delegateResult).when(delegateService).queueTask(any());

    when(mockContext.renderExpression(anyString())).thenAnswer((Answer<String>) invocation -> {
      Object[] args = invocation.getArguments();
      return (String) args[0];
    });
    if (!successEnequeueDelegateTask) {
      doThrow(Exception.class).when(delegateService).queueTask(any());
    }
    return mockContext;
  }

  private AzureTaskExecutionResponse initializeDelegateResponse(
      boolean isSuccess, ExecutionContextImpl mockContext, boolean genericFailure) {
    AzureWebAppSwapSlotsResponse swapSlotsResponse =
        AzureWebAppSwapSlotsResponse.builder()
            .preDeploymentData(AzureAppServicePreDeploymentData.builder().build())
            .build();
    AzureTaskExecutionResponse taskExecutionResponse =
        AzureTaskExecutionResponse.builder()
            .delegateMetaInfo(DelegateMetaInfo.builder().build())
            .commandExecutionStatus(isSuccess ? CommandExecutionStatus.SUCCESS : CommandExecutionStatus.FAILURE)
            .azureTaskResponse(swapSlotsResponse)
            .build();
    doReturn(isSuccess ? SUCCESS : FAILED)
        .when(azureVMSSStateHelper)
        .getAppServiceExecutionStatus(eq(taskExecutionResponse));

    if (genericFailure) {
      doThrow(Exception.class).when(azureVMSSStateHelper).getAppServiceExecutionStatus(eq(taskExecutionResponse));
    } else {
      doReturn(isSuccess ? SUCCESS : FAILED)
          .when(azureVMSSStateHelper)
          .getAppServiceExecutionStatus(eq(taskExecutionResponse));
    }
    doReturn(AzureAppServiceSlotSwapExecutionData.builder().build()).when(mockContext).getStateExecutionData();
    return taskExecutionResponse;
  }

  private void initializeStateFields() {
    state.setSubscriptionId("swapSlotSubId");
    state.setResourceGroup(SWAP_RESOURCE_GROUP);
    state.setWebApp(SWAP_APP_NAME);
    state.setTargetSlot(SWAP_TARGET_SLOT);
  }

  private void assertSuccessExecution(ExecutionResponse response) {
    verifyStateExecutionData(response);
    verifyDelegateTaskCreationResult(response);
  }

  private void verifyStateExecutionData(ExecutionResponse response) {
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isNull();
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData()).isInstanceOf(AzureAppServiceSlotSwapExecutionData.class);

    AzureAppServiceSlotSwapExecutionData stateExecutionData =
        (AzureAppServiceSlotSwapExecutionData) response.getStateExecutionData();
    assertThat(stateExecutionData.equals(new AzureAppServiceSlotSwapExecutionData())).isFalse();
    assertThat(stateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(stateExecutionData.getResourceGroup()).isEqualTo(SWAP_RESOURCE_GROUP);
    assertThat(stateExecutionData.getAppServiceName()).isEqualTo(SWAP_APP_NAME);
    assertThat(stateExecutionData.getDeploymentSlot()).isEqualTo(SWAP_DEPLOYMENT_SLOT);
    assertThat(stateExecutionData.getTargetSlot()).isEqualTo(SWAP_TARGET_SLOT);
    assertThat(stateExecutionData.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);

    AzureAppServiceSlotSwapExecutionSummary stepExecutionSummary = stateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary.equals(AzureAppServiceSlotSwapExecutionSummary.builder().build())).isFalse();
    assertThat(stateExecutionData.getStepExecutionSummary().toString()).isNotNull();

    assertThat(stateExecutionData.getExecutionDetails()).isNotEmpty();
    assertThat(stateExecutionData.getExecutionSummary()).isNotEmpty();
    assertThat(stateExecutionData.getStepExecutionSummary()).isNotNull();
  }

  private void verifyDelegateTaskCreationResult(ExecutionResponse response) {
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());

    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(1).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof AzureTaskExecutionRequest).isTrue();
    AzureTaskExecutionRequest executionRequestParams =
        (AzureTaskExecutionRequest) delegateTask.getData().getParameters()[0];

    assertThat(executionRequestParams.getAzureTaskParameters() instanceof AzureWebAppSwapSlotsParameters).isTrue();
    AzureWebAppSwapSlotsParameters swapSlotsParameters =
        (AzureWebAppSwapSlotsParameters) executionRequestParams.getAzureTaskParameters();

    assertThat(swapSlotsParameters.getResourceGroupName()).isEqualTo(SWAP_RESOURCE_GROUP);
    assertThat(swapSlotsParameters.getAppName()).isEqualTo(SWAP_APP_NAME);
    assertThat(swapSlotsParameters.getTargetSlotName()).isEqualTo(SWAP_TARGET_SLOT);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }
}
