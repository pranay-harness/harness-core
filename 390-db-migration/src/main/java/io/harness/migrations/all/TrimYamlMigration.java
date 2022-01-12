/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTask.ContainerTaskKeys;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrimYamlMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<ContainerTask> containerTasks = wingsPersistence.createQuery(ContainerTask.class)
                                             .filter(ContainerTaskKeys.deploymentType, "KUBERNETES")
                                             .field("advancedConfig")
                                             .exists()
                                             .asList();

    log.info("Trimming {} advanced yaml configs", containerTasks.size());
    for (ContainerTask containerTask : containerTasks) {
      log.info("Trimming {}", containerTask.getUuid());
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      kubernetesContainerTask.setAdvancedConfig(kubernetesContainerTask.getAdvancedConfig());
    }
    log.info("Done trimming.");

    wingsPersistence.save(containerTasks);
  }
}
