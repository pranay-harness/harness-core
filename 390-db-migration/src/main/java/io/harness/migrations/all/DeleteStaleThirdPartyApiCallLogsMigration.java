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

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallLogKeys;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public class DeleteStaleThirdPartyApiCallLogsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<String> idsToDelete = new ArrayList<>();

    int deleted = 0;
    try (HIterator<ThirdPartyApiCallLog> iterator =
             new HIterator<>(wingsPersistence.createQuery(ThirdPartyApiCallLog.class, excludeAuthority)
                                 .field(ThirdPartyApiCallLogKeys.validUntil)
                                 .doesNotExist()
                                 .fetch())) {
      while (iterator.hasNext()) {
        final ThirdPartyApiCallLog thirdPartyApiCallLog = iterator.next();
        idsToDelete.add(thirdPartyApiCallLog.getUuid());

        deleted++;
        if (deleted != 0 && idsToDelete.size() % 10000 == 0) {
          wingsPersistence.delete(
              wingsPersistence.createQuery(ThirdPartyApiCallLog.class).field("_id").in(idsToDelete));
          log.info("deleted: " + deleted);
          idsToDelete.clear();
          sleep(ofMillis(3000));
        }
      }

      if (!idsToDelete.isEmpty()) {
        wingsPersistence.delete(wingsPersistence.createQuery(ThirdPartyApiCallLog.class).field("_id").in(idsToDelete));
        log.info("deleted: " + deleted);
      }
    }

    log.info("Complete. Deleted " + deleted + " records.");
  }
}
