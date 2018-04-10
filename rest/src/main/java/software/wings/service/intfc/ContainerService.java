package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.Map;

public interface ContainerService {
  @DelegateTaskType(TaskType.CONTAINER_ACTIVE_SERVICE_COUNTS)
  Map<String, Integer> getActiveServiceCounts(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_INFO)
  List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_CONNECTION_VALIDATION)
  Boolean validate(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.FETCH_CONTAINER_INFO)
  List<software.wings.cloudprovider.ContainerInfo> fetchContainerInfos(ContainerServiceParams containerServiceParams);
}
