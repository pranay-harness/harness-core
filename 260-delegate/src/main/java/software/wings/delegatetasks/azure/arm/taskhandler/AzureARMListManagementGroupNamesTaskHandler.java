package software.wings.delegatetasks.azure.arm.taskhandler;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.management.ManagementGroupInfo;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;
import io.harness.delegate.task.azure.arm.response.AzureARMListManagementGroupNamesResponse;

import software.wings.delegatetasks.azure.arm.AbstractAzureARMTaskHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureARMListManagementGroupNamesTaskHandler extends AbstractAzureARMTaskHandler {
  @Inject private AzureManagementClient azureManagementClient;

  @Override
  protected AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    List<ManagementGroupInfo> managementGroupInfos = azureManagementClient.listManagementGroupNames(azureConfig);
    return AzureARMListManagementGroupNamesResponse.builder()
        .mngGroupNames(toManagementGroupNames(managementGroupInfos))
        .build();
  }

  private List<String> toManagementGroupNames(List<ManagementGroupInfo> managementGroupInfos) {
    return managementGroupInfos.stream().map(ManagementGroupInfo::getName).collect(Collectors.toList());
  }
}
