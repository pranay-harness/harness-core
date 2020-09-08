package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.VMSS_AUTH_TYPE_SSH_PUBLIC_KEY;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.rule.OwnerRule.IVAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVMs;
import com.microsoft.azure.management.resources.Subscription;
import io.harness.azure.client.AzureAutoScaleSettingsClient;
import io.harness.azure.client.AzureComputeClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public class AzureVMSSSetupTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AzureComputeClient mockAzureComputeClient;
  @Mock private AzureAutoScaleSettingsClient azureAutoScaleSettingsClient;
  @Mock private TimeLimiter timeLimiter;
  @Mock VirtualMachineScaleSetVMs virtualMachineScaleSetVMs;
  @Mock VirtualMachineScaleSetVM virtualMachineScaleSetVM;
  @Mock Subscription subscription;

  @Spy @InjectMocks AzureVMSSSetupTaskHandler azureVMSSSetupTaskHandler;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternal() throws Exception {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion1 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion2 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion3 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion4 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion5 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet baseVirtualMachineScaleSet = mock(VirtualMachineScaleSet.class);
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureVMSSSetupTaskParameters azureVMSSSetupTaskParameters = AzureVMSSSetupTaskParameters.builder()
                                                                    .infraMappingId("infraMappingId")
                                                                    .appId("appId")
                                                                    .accountId("accountId")
                                                                    .activityId("activityId")
                                                                    .artifactRevision("artifactRevision")
                                                                    .autoScalingSteadyStateVMSSTimeout(0)
                                                                    .baseVMSSName("baseVMSSName")
                                                                    .blueGreen(false)
                                                                    .commandName("commandName")
                                                                    .commandType(AZURE_VMSS_SETUP)
                                                                    .desiredInstances(1)
                                                                    .maxInstances(1)
                                                                    .minInstances(0)
                                                                    .resourceGroupName("resourceGroupName")
                                                                    .sshPublicKey("sshPublicKey")
                                                                    .subscriptionId("subscriptionId")
                                                                    .useCurrentRunningCount(false)
                                                                    .userName("userName")
                                                                    .vmssAuthType(VMSS_AUTH_TYPE_SSH_PUBLIC_KEY)
                                                                    .vmssNamePrefix("newVirtualScaleSetName")
                                                                    .useCurrentRunningCount(false)
                                                                    .build();

    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockCallback).saveExecutionLog(anyString(), any(), any());
    doReturn(Boolean.TRUE).when(timeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
    doReturn(mockCallback).when(azureVMSSSetupTaskHandler).getLogCallBack(any(), anyString());

    Instant instant = Instant.now();
    Date dateVersion1 = Date.from(instant.minus(5, ChronoUnit.DAYS));
    when(virtualMachineScaleSetVersion1.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__1");
        put(VMSS_CREATED_TIME_STAMP_TAG_NAME, AzureResourceUtility.dateToISO8601BasicStr(dateVersion1));
      }
    });
    doReturn("virtualMachineScaleSetVersion1ShouldBeDeleted").when(virtualMachineScaleSetVersion1).name();
    doReturn("virtualMachineScaleSetVersion1ShouldBeDeletedId").when(virtualMachineScaleSetVersion1).id();

    Date dateVersion2 = Date.from(instant.minus(4, ChronoUnit.DAYS));
    when(virtualMachineScaleSetVersion2.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__2");
        put(VMSS_CREATED_TIME_STAMP_TAG_NAME, AzureResourceUtility.dateToISO8601BasicStr(dateVersion2));
      }
    });
    doReturn(1).when(virtualMachineScaleSetVersion2).capacity();
    doReturn("virtualMachineScaleSetVersion2ShouldBeDownSizedId").when(virtualMachineScaleSetVersion2).id();
    doReturn("virtualMachineScaleSetVersion2ShouldBeDownSized").when(virtualMachineScaleSetVersion2).name();

    Date dateVersion3 = Date.from(instant.minus(3, ChronoUnit.DAYS));
    when(virtualMachineScaleSetVersion3.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__3");
        put(VMSS_CREATED_TIME_STAMP_TAG_NAME, AzureResourceUtility.dateToISO8601BasicStr(dateVersion3));
      }
    });
    doReturn(2).when(virtualMachineScaleSetVersion3).capacity();
    doReturn("mostRecentActiveVMSSName").when(virtualMachineScaleSetVersion3).name();
    doReturn("mostRecentActiveVMSSId").when(virtualMachineScaleSetVersion3).id();
    doReturn("resourceGroupName").when(virtualMachineScaleSetVersion3).resourceGroupName();

    Date dateVersion4 = Date.from(instant.minus(2, ChronoUnit.DAYS));
    when(virtualMachineScaleSetVersion4.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__4");
        put(VMSS_CREATED_TIME_STAMP_TAG_NAME, AzureResourceUtility.dateToISO8601BasicStr(dateVersion4));
      }
    });
    doReturn("virtualMachineScaleSetVersion4ShouldBeDownSizedId").when(virtualMachineScaleSetVersion4).id();
    doReturn("virtualMachineScaleSetVersion4ShouldBeDownSized").when(virtualMachineScaleSetVersion4).name();

    Date dateVersion5 = Date.from(instant.minus(1, ChronoUnit.DAYS));
    when(virtualMachineScaleSetVersion5.tags()).thenReturn(new HashMap<String, String>() {
      {
        put(HARNESS_AUTOSCALING_GROUP_TAG_NAME, "infraMappingId__5");
        put(VMSS_CREATED_TIME_STAMP_TAG_NAME, AzureResourceUtility.dateToISO8601BasicStr(dateVersion5));
      }
    });
    doReturn("virtualMachineScaleSetVersion5ShouldBeRetained").when(virtualMachineScaleSetVersion5).name();
    doReturn("virtualMachineScaleSetVersion5ShouldBeRetainedId").when(virtualMachineScaleSetVersion5).id();

    doReturn("baseVirtualMachineScaleSetId").when(baseVirtualMachineScaleSet).id();

    // downsizeOrDeleteOlderVirtualMachineScaleSets
    doReturn(Arrays.asList(virtualMachineScaleSetVersion1, virtualMachineScaleSetVersion2,
                 virtualMachineScaleSetVersion3, virtualMachineScaleSetVersion4, virtualMachineScaleSetVersion5))
        .when(mockAzureComputeClient)
        .listVirtualMachineScaleSetsByResourceGroupName(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"));
    doReturn(virtualMachineScaleSetVersion4)
        .when(mockAzureComputeClient)
        .updateVMSSCapacity(
            any(), eq("virtualMachineScaleSetVersion4ShouldBeDownSized"), anyString(), anyString(), anyInt());
    doReturn(virtualMachineScaleSetVersion2)
        .when(mockAzureComputeClient)
        .updateVMSSCapacity(
            any(), eq("virtualMachineScaleSetVersion2ShouldBeDownSized"), anyString(), anyString(), anyInt());
    doNothing()
        .when(mockAzureComputeClient)
        .deleteVirtualMachineScaleSetByResourceGroupName(any(), anyString(), anyString());

    // createVirtualMachineScaleSet
    doReturn(Optional.of(baseVirtualMachineScaleSet))
        .when(mockAzureComputeClient)
        .getVirtualMachineScaleSetByName(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), anyString());
    doReturn(Optional.of(virtualMachineScaleSetVersion3))
        .when(mockAzureComputeClient)
        .getVirtualMachineScaleSetByName(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), eq("mostRecentActiveVMSSName"));
    doNothing()
        .when(mockAzureComputeClient)
        .createVirtualMachineScaleSet(any(AzureConfig.class), any(VirtualMachineScaleSet.class), eq("infraMappingId"),
            anyString(), anyInt(), any(AzureUserAuthVMInstanceData.class), anyBoolean());

    // buildAzureVMSSSetupTaskResponse
    doReturn(Optional.of("{baseScalingPolicies: {...}}"))
        .when(azureAutoScaleSettingsClient)
        .getAutoScaleSettingJSONByTargetResourceId(
            any(AzureConfig.class), eq("resourceGroupName"), eq("baseVirtualMachineScaleSetId"));

    // populatePreDeploymentData
    doReturn(Optional.of("{mostRecentScalingPolicies: {...}}"))
        .when(azureAutoScaleSettingsClient)
        .getAutoScaleSettingJSONByTargetResourceId(
            any(AzureConfig.class), eq("resourceGroupName"), eq("mostRecentActiveVMSSId"));

    ArgumentCaptor<String> baseVirtualScaleSetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> newVirtualScaleSetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> harnessRevisionArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

    AzureVMSSTaskExecutionResponse response =
        azureVMSSSetupTaskHandler.executeTaskInternal(azureVMSSSetupTaskParameters, azureConfig);

    // createVirtualMachineScaleSet
    verify(mockAzureComputeClient, times(3))
        .getVirtualMachineScaleSetByName(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), anyString());

    verify(mockAzureComputeClient, times(1))
        .createVirtualMachineScaleSet(any(AzureConfig.class), any(VirtualMachineScaleSet.class), eq("infraMappingId"),
            anyString(), anyInt(), any(AzureUserAuthVMInstanceData.class), anyBoolean());

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskResponse).isNotNull();
    assertThat(azureVMSSTaskResponse instanceof AzureVMSSSetupTaskResponse).isTrue();
    AzureVMSSSetupTaskResponse setupResponse = (AzureVMSSSetupTaskResponse) azureVMSSTaskResponse;
    assertThat(setupResponse.getErrorMessage()).isNull();
    assertThat(setupResponse.getBaseVMSSScalingPolicyJSONs())
        .isEqualTo(Collections.singletonList("{baseScalingPolicies: {...}}"));
    assertThat(setupResponse.getDesiredInstances()).isEqualTo(1);
    assertThat(setupResponse.getHarnessRevision()).isEqualTo(6);
    assertThat(setupResponse.getLastDeployedVMSSName()).isEqualTo("mostRecentActiveVMSSName");
    assertThat(setupResponse.getMaxInstances()).isEqualTo(1);
    assertThat(setupResponse.getMinInstances()).isEqualTo(0);
    assertThat(setupResponse.getNewVirtualMachineScaleSetName()).isEqualTo("newVirtualScaleSetName__6");

    // for rollback
    assertThat(setupResponse.getPreDeploymentData().getDesiredCapacity()).isEqualTo(2);
    assertThat(setupResponse.getPreDeploymentData().getMinCapacity()).isEqualTo(0);
    assertThat(setupResponse.getPreDeploymentData().getOldVmssName()).isEqualTo("mostRecentActiveVMSSName");
    assertThat(setupResponse.getPreDeploymentData().getScalingPolicyJSON())
        .isEqualTo(Collections.singletonList("{mostRecentScalingPolicies: {...}}"));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithNotValidRequest() throws Exception {
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureVMSSTaskParameters azureVMSSDeployTaskParameters = AzureVMSSDeployTaskParameters.builder().build();

    AzureVMSSTaskExecutionResponse response =
        azureVMSSSetupTaskHandler.executeTaskInternal(azureVMSSDeployTaskParameters, azureConfig);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getErrorMessage()).isNotNull();
  }
}
