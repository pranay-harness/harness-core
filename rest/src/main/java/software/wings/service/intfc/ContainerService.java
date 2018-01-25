package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ContainerServiceParams;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public interface ContainerService {
  @DelegateTaskType(TaskType.CONTAINER_SERVICE_DESIRED_COUNT)
  Optional<Integer> getServiceDesiredCount(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_ACTIVE_SERVICE_COUNTS)
  LinkedHashMap<String, Integer> getActiveServiceCounts(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_DAEMON_SET_YAML)
  String getDaemonSetYaml(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_ACTIVE_AUTOSCALERS)
  List<String> getActiveAutoscalers(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_INFO)
  List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_CONNECTION_VALIDATION)
  Boolean validate(ContainerServiceParams containerServiceParams);
}
