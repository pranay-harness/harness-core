/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public class PrometheusCVMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    int updated = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority)
                                 .filter(CVConfigurationKeys.stateType, StateType.PROMETHEUS)
                                 .fetch())) {
      while (iterator.hasNext()) {
        try {
          final PrometheusCVServiceConfiguration prometheusCVConfiguration =
              (PrometheusCVServiceConfiguration) iterator.next();

          log.info("running migration for {} ", prometheusCVConfiguration);
          if (isEmpty(prometheusCVConfiguration.getTimeSeriesToAnalyze())) {
            log.error("Empty timeseries list for prometheus {}", prometheusCVConfiguration);
            continue;
          }

          prometheusCVConfiguration.getTimeSeriesToAnalyze().forEach(timeSeries -> {
            final String originalUrl = timeSeries.getUrl();
            String updatedUrl = originalUrl.replace("/api/v1/query_range?", "");
            updatedUrl = updatedUrl.replace("api/v1/query_range?", "");
            updatedUrl = updatedUrl.replace("/api/v1/query?", "");
            updatedUrl = updatedUrl.replace("api/v1/query?", "");
            updatedUrl = updatedUrl.replace("start=$startTime", "");
            updatedUrl = updatedUrl.replace("end=$endTime", "");
            updatedUrl = updatedUrl.replace("step=60s", "");
            updatedUrl = updatedUrl.replace("query=", "");
            updatedUrl = updatedUrl.replaceAll("&", "");
            log.info("for {} \nreplacing: {} \nwith: {}", prometheusCVConfiguration.getUuid(), originalUrl, updatedUrl);
            timeSeries.setUrl(updatedUrl);
          });
          wingsPersistence.save(prometheusCVConfiguration);
          updated++;
        } catch (Exception e) {
          log.info("Error while running migration", e);
        }
      }
    }

    log.info("Complete. updated " + updated + " records.");
  }
}
