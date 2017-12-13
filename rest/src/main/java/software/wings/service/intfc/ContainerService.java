package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;

public interface ContainerService {
  @DelegateTaskType(TaskType.CONTAINER_INFO)
  List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams);
}
