package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.HARNESS_AUTOSCALING_GROUP_TAG_NAME;
import static io.harness.azure.model.AzureConstants.VMSS_CREATED_TIME_STAMP_TAG_NAME;
import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP;
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

import io.harness.azure.client.AzureAutoScaleSettingsClient;
import io.harness.azure.client.AzureComputeClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureMachineImageArtifact;
import io.harness.azure.model.AzureUserAuthVMInstanceData;
import io.harness.azure.model.AzureVMSSAutoScaleSettingsData;
import io.harness.azure.model.AzureVMSSTagsData;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.AzureVMAuthType;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import com.google.common.util.concurrent.TimeLimiter;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVMs;
import com.microsoft.azure.management.resources.Subscription;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AzureVMSSSetupTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AzureComputeClient mockAzureComputeClient;
  @Mock private AzureAutoScaleSettingsClient mockAzureAutoScaleSettingsClient;
  @Mock private AzureAutoScaleHelper mockAzureAutoScaleHelper;
  @Mock private TimeLimiter timeLimiter;
  @Mock VirtualMachineScaleSetVMs virtualMachineScaleSetVMs;
  @Mock VirtualMachineScaleSetVM virtualMachineScaleSetVM;
  @Mock Subscription subscription;
  private final int minInstances = 0;
  private final int maxInstances = 2;
  private final int desiredInstances = 1;

  @Spy @InjectMocks AzureVMSSSetupTaskHandler azureVMSSSetupTaskHandler;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternal() throws Exception {
    mockAzureAutoScaleHelperMethods();
    mockExecutionLogCallbackMethods();
    VirtualMachineScaleSet virtualMachineScaleSetVersion1 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion2 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion3 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion4 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet virtualMachineScaleSetVersion5 = mock(VirtualMachineScaleSet.class);
    VirtualMachineScaleSet baseVirtualMachineScaleSet = mock(VirtualMachineScaleSet.class);
    AzureConfig azureConfig = AzureConfig.builder().build();
    AzureVMSSSetupTaskParameters azureVMSSSetupTaskParameters = buildAzureVMSSSetupTaskParameters();
    AzureMachineImageArtifact azureMachineImageArtifact = mock(AzureMachineImageArtifact.class);

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
    doNothing()
        .when(azureVMSSSetupTaskHandler)
        .updateVMSSCapacityAndWaitForSteadyState(any(AzureConfig.class), any(AzureVMSSTaskParameters.class),
            anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString(), anyString());
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
        .deleteVirtualMachineScaleSetByResourceGroupName(any(), anyString(), anyString(), anyString());

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
        .createVirtualMachineScaleSet(any(AzureConfig.class), anyString(), any(VirtualMachineScaleSet.class),
            anyString(), any(AzureUserAuthVMInstanceData.class), any(AzureMachineImageArtifact.class),
            any(AzureVMSSTagsData.class));
    // buildAzureVMSSSetupTaskResponse
    doReturn(Optional.of("{baseScalingPolicies: {...}}"))
        .when(mockAzureAutoScaleSettingsClient)
        .getAutoScaleSettingJSONByTargetResourceId(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), eq("baseVirtualMachineScaleSetId"));

    // populatePreDeploymentData
    doReturn(Optional.of("{mostRecentScalingPolicies: {...}}"))
        .when(mockAzureAutoScaleSettingsClient)
        .getAutoScaleSettingJSONByTargetResourceId(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), eq("mostRecentActiveVMSSId"));

    doReturn(azureMachineImageArtifact)
        .when(azureVMSSSetupTaskHandler)
        .getAzureMachineImageArtifact(any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"),
            any(AzureMachineImageArtifactDTO.class), any());

    AzureVMSSTaskExecutionResponse response =
        azureVMSSSetupTaskHandler.executeTaskInternal(azureVMSSSetupTaskParameters, azureConfig);

    // createVirtualMachineScaleSet
    verify(mockAzureComputeClient, times(1))
        .getVirtualMachineScaleSetByName(
            any(AzureConfig.class), eq("subscriptionId"), eq("resourceGroupName"), anyString());

    verify(mockAzureComputeClient, times(1))
        .createVirtualMachineScaleSet(any(AzureConfig.class), anyString(), any(VirtualMachineScaleSet.class),
            anyString(), any(AzureUserAuthVMInstanceData.class), any(AzureMachineImageArtifact.class),
            any(AzureVMSSTagsData.class));

    assertThat(response).isNotNull();
    AzureVMSSTaskResponse azureVMSSTaskResponse = response.getAzureVMSSTaskResponse();
    assertThat(azureVMSSTaskResponse).isNotNull();
    assertThat(azureVMSSTaskResponse instanceof AzureVMSSSetupTaskResponse).isTrue();
    AzureVMSSSetupTaskResponse setupResponse = (AzureVMSSSetupTaskResponse) azureVMSSTaskResponse;
    assertThat(setupResponse.getErrorMessage()).isNull();
    assertThat(setupResponse.getBaseVMSSScalingPolicyJSONs())
        .isEqualTo(Collections.singletonList("{baseScalingPolicies: {...}}"));
    assertThat(setupResponse.getHarnessRevision()).isEqualTo(6);
    assertThat(setupResponse.getLastDeployedVMSSName()).isEqualTo("mostRecentActiveVMSSName");
    assertThat(setupResponse.getNewVirtualMachineScaleSetName()).isEqualTo("newVirtualScaleSetName__6");
    // instance limits
    assertThat(setupResponse.getDesiredInstances()).isEqualTo(desiredInstances);
    assertThat(setupResponse.getMaxInstances()).isEqualTo(maxInstances);
    assertThat(setupResponse.getMinInstances()).isEqualTo(minInstances);

    // for rollback
    assertThat(setupResponse.getPreDeploymentData().getDesiredCapacity()).isEqualTo(2);
    assertThat(setupResponse.getPreDeploymentData().getMinCapacity()).isEqualTo(0);
    assertThat(setupResponse.getPreDeploymentData().getOldVmssName()).isEqualTo("mostRecentActiveVMSSName");
    assertThat(setupResponse.getPreDeploymentData().getScalingPolicyJSON())
        .isEqualTo(Collections.singletonList("{mostRecentScalingPolicies: {...}}"));
  }

  private AzureVMSSSetupTaskParameters buildAzureVMSSSetupTaskParameters() {
    AzureVMAuthDTO azureVMAuthDTO = buildAzureVMAuthDTO();
    return AzureVMSSSetupTaskParameters.builder()
        .infraMappingId("infraMappingId")
        .appId("appId")
        .accountId("accountId")
        .activityId("activityId")
        .autoScalingSteadyStateVMSSTimeout(10)
        .baseVMSSName("baseVMSSName")
        .blueGreen(false)
        .commandName("commandName")
        .azureVmAuthDTO(azureVMAuthDTO)
        .commandType(AZURE_VMSS_SETUP)
        .resourceGroupName("resourceGroupName")
        .subscriptionId("subscriptionId")
        .vmssNamePrefix("newVirtualScaleSetName")
        .useCurrentRunningCount(false)
        .build();
  }

  private AzureVMAuthDTO buildAzureVMAuthDTO() {
    return AzureVMAuthDTO.builder()
        .userName("username")
        .azureVmAuthType(AzureVMAuthType.SSH_PUBLIC_KEY)
        .secretRef(SecretRefData.builder().decryptedValue("sshPublicKey".toCharArray()).build())
        .build();
  }

  private void mockAzureAutoScaleHelperMethods() {
    AzureVMSSAutoScaleSettingsData azureVMSSAutoScaleSettingsData = mock(AzureVMSSAutoScaleSettingsData.class);
    doReturn(minInstances).when(azureVMSSAutoScaleSettingsData).getMinInstances();
    doReturn(maxInstances).when(azureVMSSAutoScaleSettingsData).getMaxInstances();
    doReturn(desiredInstances).when(azureVMSSAutoScaleSettingsData).getDesiredInstances();
    doReturn(azureVMSSAutoScaleSettingsData)
        .when(mockAzureAutoScaleHelper)
        .getVMSSAutoScaleInstanceLimits(any(), any(), any(), anyBoolean(), anyString());
    doReturn(Collections.singletonList("{baseScalingPolicies: {...}}"))
        .when(mockAzureAutoScaleHelper)
        .getVMSSAutoScaleSettingsJSONs(any(), anyString(), anyString(), anyString());
    doReturn(Collections.singletonList("{mostRecentScalingPolicies: {...}}"))
        .when(mockAzureAutoScaleHelper)
        .getVMSSAutoScaleSettingsJSONs(any(), anyString(), any());
  }

  private void mockExecutionLogCallbackMethods() throws Exception {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockCallback).saveExecutionLog(anyString(), any(), any());
    doReturn(Boolean.TRUE).when(timeLimiter).callWithTimeout(any(), anyLong(), any(), anyBoolean());
    doReturn(mockCallback).when(azureVMSSSetupTaskHandler).getLogCallBack(any(), anyString());
  }
}
