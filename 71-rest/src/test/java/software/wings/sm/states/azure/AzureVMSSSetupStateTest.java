package software.wings.sm.states.azure;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.IVAN;

import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.VMSSAuthType;
import software.wings.beans.VMSSDeploymentType;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.ManagerExecutionLogCallback;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AzureVMSSSetupStateTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private AzureVMSSStateHelper azureVMSSStateHelper;
  @Mock private InfrastructureMappingService infrastructureMappingService;

  @Spy @InjectMocks AzureVMSSSetupState state = new AzureVMSSSetupState("Azure VMSS Setup State");

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecute() {
    String appId = "appId";
    String serviceId = "serviceId";
    String envId = "envId";
    String activityId = "activityId";
    String infraMappingId = "infraMappingId";
    String userData = "userData";
    String namePrefix = "namePrefix";
    String delegateResult = "Done";
    int autoScalingSteadyStateVMSSTimeoutFixed = 1;
    Integer numberOfInstances = 1;
    boolean isBlueGreen = false;
    Application app = Application.Builder.anApplication().uuid(appId).build();
    Environment env = Environment.Builder.anEnvironment().uuid(envId).build();
    Service service = Service.builder().uuid(serviceId).build();
    Activity activity = Activity.builder().uuid(activityId).build();
    InfrastructureMapping azureVMSSInfrastructureMapping = AzureVMSSInfrastructureMapping.builder()
                                                               .baseVMSSName("baseVMSSName")
                                                               .resourceGroupName("resourceGroupName")
                                                               .subscriptionId("subscriptionId")
                                                               .passwordSecretTextName("password")
                                                               .userName("userName")
                                                               .vmssAuthType(VMSSAuthType.PASSWORD)
                                                               .vmssDeploymentType(VMSSDeploymentType.NATIVE_VMSS)
                                                               .build();
    AzureConfig azureConfig = AzureConfig.builder().build();
    Artifact artifact = Artifact.Builder.anArtifact().build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    ManagerExecutionLogCallback managerExecutionLogCallback = mock(ManagerExecutionLogCallback.class);

    doReturn(app).when(azureVMSSStateHelper).getApplication(context);
    doReturn(env).when(azureVMSSStateHelper).getEnvironment(context);
    doReturn(service).when(azureVMSSStateHelper).getServiceByAppId(any(), anyString());
    doReturn(activity)
        .when(azureVMSSStateHelper)
        .createAndSaveActivity(any(), any(), anyString(), anyString(), any(), anyList());
    doReturn(managerExecutionLogCallback).when(azureVMSSStateHelper).getExecutionLogCallback(activity);
    doReturn(infraMappingId).when(context).fetchInfraMappingId();
    doReturn(azureVMSSInfrastructureMapping).when(infrastructureMappingService).get(infraMappingId, appId);
    doReturn(autoScalingSteadyStateVMSSTimeoutFixed)
        .when(azureVMSSStateHelper)
        .renderTimeoutExpressionOrGetDefault(anyString(), any(), anyInt());
    doReturn(azureVMSSInfrastructureMapping)
        .when(azureVMSSStateHelper)
        .getAzureVMSSInfrastructureMapping(anyString(), anyString());
    doReturn(azureConfig)
        .when(azureVMSSStateHelper)
        .getAzureConfig(azureVMSSInfrastructureMapping.getComputeProviderSettingId());
    doReturn(encryptedDataDetails)
        .when(azureVMSSStateHelper)
        .getEncryptedDataDetails(context, azureVMSSInfrastructureMapping.getComputeProviderSettingId());
    doReturn(artifact).when(azureVMSSStateHelper).getArtifact(any(), any());
    doReturn(isBlueGreen).when(azureVMSSStateHelper).isBlueGreenWorkflow(context);
    doReturn(userData).when(azureVMSSStateHelper).getBase64EncodedUserData(context, appId, serviceId);
    doReturn(namePrefix)
        .when(azureVMSSStateHelper)
        .fixNamePrefix(any(), anyString(), anyString(), anyString(), anyString());
    doReturn(numberOfInstances).when(azureVMSSStateHelper).renderExpressionOrGetDefault(anyString(), any(), anyInt());
    doReturn(delegateResult).when(delegateService).queueTask(any());

    ExecutionResponse result = state.execute(context);

    assertThat(result).isNotNull();
    assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getStateExecutionData()).isNotNull();
    assertThat(result.getStateExecutionData()).isInstanceOf(AzureVMSSSetupStateExecutionData.class);
    assertThat(((AzureVMSSSetupStateExecutionData) result.getStateExecutionData()).getActivityId())
        .isEqualTo(activityId);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    String newVirtualMachineScaleSetName = "newVirtualMachineScaleSetName";
    String oldVirtualMachineScaleSetName = "oldVirtualMachineScaleSetName";
    String lastDeployedVMSSName = "lastDeployedVMSSName";
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    doNothing().when(azureVMSSStateHelper).updateActivityStatus(anyString(), anyString(), any());
    doReturn(false).when(azureVMSSStateHelper).isBlueGreenWorkflow(context);
    doReturn(5).when(azureVMSSStateHelper).renderTimeoutExpressionOrGetDefault(anyString(), any(), anyInt());
    doReturn(SUCCESS).when(azureVMSSStateHelper).getExecutionStatus(any());
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID,
        AzureVMSSTaskExecutionResponse.builder()
            .azureVMSSTaskResponse(
                AzureVMSSSetupTaskResponse.builder()
                    .lastDeployedVMSSName(lastDeployedVMSSName)
                    .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
                    .maxInstances(1)
                    .minInstances(1)
                    .desiredInstances(1)
                    .preDeploymentData(AzureVMSSPreDeploymentData.builder().desiredCapacity(1).build())
                    .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build());
    AzureVMSSSetupStateExecutionData data = AzureVMSSSetupStateExecutionData.builder()
                                                .newVirtualMachineScaleSetName(newVirtualMachineScaleSetName)
                                                .oldVirtualMachineScaleSetName(oldVirtualMachineScaleSetName)
                                                .maxInstances(1)
                                                .desiredInstances(1)
                                                .build();
    doReturn(data).when(context).getStateExecutionData();

    ExecutionResponse executionResponse = state.handleAsyncResponse(context, responseMap);

    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> notifyElements = executionResponse.getNotifyElements();
    assertThat(notifyElements).isNotNull();
    assertThat(notifyElements.size()).isEqualTo(1);
    ContextElement contextElement = notifyElements.get(0);
    assertThat(contextElement).isNotNull();
    assertThat(contextElement instanceof AzureVMSSSetupContextElement).isTrue();
    AzureVMSSSetupContextElement azureVMSSSetupContextElement = (AzureVMSSSetupContextElement) contextElement;
    assertThat(azureVMSSSetupContextElement.getNewVirtualMachineScaleSetName())
        .isEqualTo(newVirtualMachineScaleSetName);
    assertThat(azureVMSSSetupContextElement.getOldVirtualMachineScaleSetName()).isEqualTo(lastDeployedVMSSName);
  }
}
