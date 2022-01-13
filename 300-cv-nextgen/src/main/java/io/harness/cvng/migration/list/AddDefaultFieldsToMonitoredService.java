/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class AddDefaultFieldsToMonitoredService implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Override
  public void migrate() {
    Set<ServiceEnvironmentParams> serviceEnvironmentParams =
        hPersistence.createQuery(CVConfig.class)
            .asList()
            .stream()
            .map(cvConfig
                -> ServiceEnvironmentParams.builder()
                       .projectIdentifier(cvConfig.getProjectIdentifier())
                       .accountIdentifier(cvConfig.getAccountId())
                       .orgIdentifier(cvConfig.getOrgIdentifier())
                       .serviceIdentifier(cvConfig.getServiceIdentifier())
                       .environmentIdentifier(cvConfig.getEnvIdentifier())
                       .build())
            .collect(Collectors.toSet());
    serviceEnvironmentParams.forEach(serviceEnvironmentParam -> {
      log.info("Migrating monitored service for : %s", serviceEnvironmentParam);
      monitoredServiceService.update(serviceEnvironmentParam.getAccountIdentifier(),
          monitoredServiceService.getMonitoredServiceDTO(serviceEnvironmentParam));
      log.info("Done migrating monitored service for : %s", serviceEnvironmentParam);
    });
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.builder().desc("Adding a new field so it won't impact the rollback").build();
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.builder().desc("Adding a new field so old version also can process the entity").build();
  }
}
