/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InvalidCVConfigDeletionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void migrate() {
    log.info("Staring InvalidCVConfigDeletionMigration");
    int deleted = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        try {
          final CVConfiguration cvConfiguration = iterator.next();
          Service service = serviceResourceService.get(cvConfiguration.getServiceId());
          Environment environment = environmentService.get(cvConfiguration.getAppId(), cvConfiguration.getEnvId());

          if (service == null || environment == null) {
            log.info("for {} deleting {} with id {}, service: {} environment: {}", cvConfiguration.getAccountId(),
                cvConfiguration.getName(), cvConfiguration.getUuid(), service, environment);
            cvConfigurationService.deleteConfiguration(
                cvConfiguration.getAccountId(), cvConfiguration.getAppId(), cvConfiguration.getUuid());
            deleted++;
          }
          sleep(ofMillis(500));
        } catch (Exception e) {
          log.info("Error while running migration", e);
        }
      }
    }
    log.info("Complete. deleted " + deleted + " records.");
  }
}
