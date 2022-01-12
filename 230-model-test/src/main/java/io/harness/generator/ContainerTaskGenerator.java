/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.govern.Switch.unhandled;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Application;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTask.ContainerTaskKeys;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.PortMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class ContainerTaskGenerator {
  @Inject ApplicationGenerator applicationGenerator;
  @Inject ServiceResourceService serviceResourceService;
  @Inject WingsPersistence wingsPersistence;

  public enum ContainerTasks { ECS_CONTAINER_PORT_MAPPING }

  public ContainerTask ensurePredefined(Randomizer.Seed seed, OwnerManager.Owners owners,
      ContainerTaskGenerator.ContainerTasks predefined, String serviceId) {
    switch (predefined) {
      case ECS_CONTAINER_PORT_MAPPING:
        return ensureEcsContainerPortMapping(seed, owners, serviceId);
      default:
        unhandled(predefined);
    }
    return null;
  }

  public ContainerTask ensureEcsContainerPortMapping(
      Randomizer.Seed seed, OwnerManager.Owners owners, String serviceId) {
    Application application = owners.obtainApplication(
        () -> applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST));
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder().portMappings(asList(portMapping)).memory(256).cpu(128.0).build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));

    ecsContainerTask.setAppId(application.getAppId());
    ecsContainerTask.setServiceId(serviceId);
    ecsContainerTask.setCreatedBy(owners.obtainUser());
    owners.add(ensureEcsContainerTask(ecsContainerTask));
    return ecsContainerTask;
  }

  public ContainerTask exists(ContainerTask containerTask) {
    return wingsPersistence.createQuery(ContainerTask.class)
        .filter(ContainerTaskKeys.serviceId, containerTask.getServiceId())
        .filter(ContainerTaskKeys.deploymentType, containerTask.getDeploymentType())
        .get();
  }

  public ContainerTask ensureEcsContainerTask(ContainerTask containerTask) {
    return GeneratorUtils.suppressDuplicateException(
        () -> serviceResourceService.createContainerTask(containerTask, false), () -> exists(containerTask));
  }
}
