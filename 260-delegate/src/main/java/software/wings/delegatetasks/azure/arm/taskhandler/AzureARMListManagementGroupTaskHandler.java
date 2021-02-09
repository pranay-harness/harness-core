package software.wings.delegatetasks.azure.arm.taskhandler;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
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
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureARMListManagementGroupTaskHandler extends AbstractAzureARMTaskHandler {
  @Inject private AzureManagementClient azureManagementClient;

  @Override
  protected AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    List<ManagementGroupInfo> managementGroupInfos = azureManagementClient.listManagementGroupNames(azureConfig);
    return AzureARMListManagementGroupNamesResponse.builder()
        .mngGroupNames(toManagementGroupIds(managementGroupInfos))
        .build();
  }

  private List<String> toManagementGroupIds(List<ManagementGroupInfo> managementGroupInfos) {
    return managementGroupInfos.stream().map(group -> fixGroupId(group.getId())).collect(Collectors.toList());
  }

  @NotNull
  private String fixGroupId(String groupId) {
    return groupId.replace(AzureConstants.MANAGEMENT_GROUP_PROVIDERS_PREFIX, EMPTY);
  }
}
