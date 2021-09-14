/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;

import software.wings.beans.TaskType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.MasterUrlFetchTaskParameter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ContainerService {
  @DelegateTaskType(TaskType.CONTAINER_ACTIVE_SERVICE_COUNTS)
  Map<String, Integer> getActiveServiceCounts(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_INFO)
  List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams, boolean isInstanceSync);

  @DelegateTaskType(TaskType.CONTROLLER_NAMES_WITH_LABELS)
  Set<String> getControllerNames(ContainerServiceParams containerServiceParams, Map<String, String> labels);

  @DelegateTaskType(TaskType.CONTAINER_CONNECTION_VALIDATION)
  Boolean validate(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CONTAINER_CE_VALIDATION) Boolean validateCE(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.CE_DELEGATE_VALIDATION)
  CEK8sDelegatePrerequisite validateCEK8sDelegate(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.LIST_CLUSTERS) List<String> listClusters(ContainerServiceParams containerServiceParams);

  @DelegateTaskType(TaskType.FETCH_MASTER_URL)
  String fetchMasterUrl(MasterUrlFetchTaskParameter masterUrlFetchTaskParameter);
}
