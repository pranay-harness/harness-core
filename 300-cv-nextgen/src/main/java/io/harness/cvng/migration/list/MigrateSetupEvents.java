/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.SetupUsageEventService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class MigrateSetupEvents implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private SetupUsageEventService setupUsageEventService;

  @Override
  public void migrate() {
    log.info("Begin migration for setup usage events");
    migrateMonitoredServiceEvents();
    log.info("Finish migration for setup usage events");
  }

  private void migrateMonitoredServiceEvents() {
    Query<MonitoredService> monitoredServiceQuery = hPersistence.createQuery(MonitoredService.class);
    try (HIterator<MonitoredService> iterator = new HIterator<>(monitoredServiceQuery.fetch())) {
      while (iterator.hasNext()) {
        MonitoredService monitoredService = iterator.next();
        ServiceEnvironmentParams serviceEnvironmentParams =
            ServiceEnvironmentParams.builder()
                .accountIdentifier(monitoredService.getAccountId())
                .orgIdentifier(monitoredService.getOrgIdentifier())
                .projectIdentifier(monitoredService.getProjectIdentifier())
                .serviceIdentifier(monitoredService.getServiceIdentifier())
                .environmentIdentifier(monitoredService.getEnvironmentIdentifier())
                .build();
        MonitoredServiceDTO monitoredServiceDTO =
            monitoredServiceService.getMonitoredServiceDTO(serviceEnvironmentParams);
        setupUsageEventService.sendCreateEventsForMonitoredService(serviceEnvironmentParams, monitoredServiceDTO);
      }
    }
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
