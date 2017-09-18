package software.wings.service.impl.instance.sync;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Task;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.request.EcsFilter;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 09/08/17
 */
public class EcsContainerSyncImpl implements ContainerSync {
  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingsService;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest) {
    EcsFilter filter = (EcsFilter) syncRequest.getFilter();
    String nextToken = null;
    SettingAttribute settingAttribute = settingsService.get(filter.getAwsComputeProviderId());
    Validator.notNullCheck("SettingAttribute", settingAttribute);
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(settingAttribute);
    Validator.notNullCheck("AwsConfig", awsConfig);
    List<Task> tasks;
    List<ContainerInfo> result = new ArrayList<>();
    Set<String> serviceNameSet = filter.getServiceNameSet();
    for (String serviceName : serviceNameSet) {
      do {
        ListTasksRequest listTasksRequest = new ListTasksRequest()
                                                .withCluster(filter.getClusterName())
                                                .withServiceName(serviceName)
                                                .withMaxResults(100)
                                                .withNextToken(nextToken)
                                                .withDesiredStatus("RUNNING");
        ListTasksResult listTasksResult = awsHelperService.listTasks(filter.getRegion(), awsConfig, listTasksRequest);

        if (!listTasksResult.getTaskArns().isEmpty()) {
          DescribeTasksRequest describeTasksRequest =
              new DescribeTasksRequest().withCluster(filter.getClusterName()).withTasks(listTasksResult.getTaskArns());
          DescribeTasksResult describeTasksResult =
              awsHelperService.describeTasks(filter.getRegion(), awsConfig, describeTasksRequest);
          tasks = describeTasksResult.getTasks();
          for (Task task : tasks) {
            EcsContainerInfo ecsContainerInfo = EcsContainerInfo.Builder.anEcsContainerInfo()
                                                    .withClusterName(filter.getClusterName())
                                                    .withTaskDefinitionArn(task.getTaskDefinitionArn())
                                                    .withTaskArn(task.getTaskArn())
                                                    .withVersion(task.getVersion())
                                                    .withStartedAt(task.getStartedAt().getTime())
                                                    .withStartedBy(task.getStartedBy())
                                                    .withServiceName(serviceName)
                                                    .build();
            result.add(ecsContainerInfo);
          }
        }
        nextToken = listTasksResult.getNextToken();
      } while (nextToken != null);
    }

    return ContainerSyncResponse.Builder.aContainerSyncResponse().withContainerInfoList(result).build();
  }
}
