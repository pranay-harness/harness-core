/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeSeriesThresholdsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void migrate() {
    int updated = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        CVConfiguration configuration = iterator.next();
        cvConfigurationService.updateConfiguration(configuration, configuration.getAppId());
        updated++;
      }
    }
    log.info("Complete. updated " + updated + " records.");
  }
}
