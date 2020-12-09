package software.wings.sm.states.azure.appservice;

import static io.harness.beans.ExecutionStatus.SKIPPED;
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
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
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
import software.wings.sm.states.azure.AzureSweepingOutputServiceHelper;
import software.wings.sm.states.azure.AzureVMSSStateHelper;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupContextElement;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionData;
import software.wings.sm.states.azure.appservices.AzureAppServiceStateData;
import software.wings.sm.states.azure.appservices.AzureWebAppSlotRollback;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;

public class AzureWebAppSlotRollbackTest extends WingsBaseTest {
  @Mock protected transient DelegateService delegateService;
  @Mock protected transient AzureVMSSStateHelper azureVMSSStateHelper;
  @Mock protected transient AzureSweepingOutputServiceHelper azureSweepingOutputServiceHelper;
  @Mock protected ActivityService activityService;
  @Spy @InjectMocks AzureWebAppSlotRollback state = new AzureWebAppSlotRollback("Web app slot rollback state");

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotRollbackExecuteSuccess() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, true);
    ExecutionResponse response = state.execute(mockContext);
    verifyStateExecutionData(response);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotAbsenceOfContextElement() {
    ExecutionContextImpl mockContext = initializeMockSetup(true, false);
    ExecutionResponse failedResponse = state.execute(mockContext);
    assertThat(failedResponse.getExecutionStatus()).isEqualTo(SKIPPED);
  }

  private ExecutionContextImpl initializeMockSetup(boolean isSuccess, boolean contextElement) {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String delegateResult = "Done";

    String ACTIVITY_ID = "activityId";
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    AzureAppServicePreDeploymentData preDeploymentData = AzureAppServicePreDeploymentData.builder().build();
    AzureAppServiceSlotSetupContextElement setupContextElement = AzureAppServiceSlotSetupContextElement.builder()
                                                                     .preDeploymentData(preDeploymentData)
                                                                     .deploymentSlot("dev-slot")
                                                                     .appServiceSlotSetupTimeOut(10)
                                                                     .build();

    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    String SWAP_DEPLOYMENT_SLOT = "swapDeploymentSlot";
    AzureWebAppInfrastructureMapping azureWebAppInfrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                            .resourceGroup("resourceGroup")
                                                                            .subscriptionId("subId")
                                                                            .webApp("app-service")
                                                                            .deploymentSlot(SWAP_DEPLOYMENT_SLOT)
                                                                            .build();

    AzureAppServiceStateData appServiceStateData = AzureAppServiceStateData.builder()
                                                       .application(app)
                                                       .environment(env)
                                                       .service(service)
                                                       .infrastructureMapping(azureWebAppInfrastructureMapping)
                                                       .deploymentSlot(SWAP_DEPLOYMENT_SLOT)
                                                       .resourceGroup("rg")
                                                       .subscriptionId("subId")
                                                       .azureConfig(azureConfig)
                                                       .artifact(artifact)
                                                       .azureEncryptedDataDetails(encryptedDataDetails)
                                                       .appService("app-service")
                                                       .build();

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    if (contextElement) {
      doReturn(setupContextElement).when(mockContext).getContextElement(eq(ContextElementType.AZURE_WEBAPP_SETUP));
      doReturn(setupContextElement)
          .when(azureSweepingOutputServiceHelper)
          .getSetupElementFromSweepingOutput(eq(mockContext), eq(AMI_SERVICE_SETUP_SWEEPING_OUTPUT_NAME));
    }
    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyListOf(CommandUnit.class));
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    doReturn(appServiceStateData).when(azureVMSSStateHelper).populateAzureAppServiceData(eq(mockContext));
    doReturn(delegateResult).when(delegateService).queueTask(any());

    when(mockContext.renderExpression(anyString())).thenAnswer((Answer<String>) invocation -> {
      Object[] args = invocation.getArguments();
      return (String) args[0];
    });
    if (!isSuccess) {
      doThrow(Exception.class).when(delegateService).queueTask(any());
    }
    return mockContext;
  }

  private void verifyStateExecutionData(ExecutionResponse response) {
    assertThat(state.isRollback()).isTrue();
    assertThat(state.getApplicationSettings()).isNull();
    assertThat(state.getAppServiceConnectionStrings()).isNull();
    assertThat(state.getSlotSteadyStateTimeout()).isNull();

    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isNull();
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData()).isInstanceOf(AzureAppServiceSlotSetupExecutionData.class);
  }
}
