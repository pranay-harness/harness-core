/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * remove duplicate entries from `instanceStats` collection.
 */
@Slf4j
public class RemoveDupInstanceStats implements Migration {
  @Inject private WingsPersistence persistence;

  @Value
  @AllArgsConstructor
  private class UniqueKey {
    private Instant timestamp;
    private String accountId;
  }

  @Override
  public void migrate() {
    log.info("Running migration - remove duplicate entries from `instanceStats` collection");

    try (HIterator<InstanceStatsSnapshot> stats =
             new HIterator<>(persistence.createQuery(InstanceStatsSnapshot.class).fetch())) {
      Set<UniqueKey> statsSnapshotSet = new HashSet<>();

      int deleteCount = 0;
      for (InstanceStatsSnapshot stat : stats) {
        boolean added = statsSnapshotSet.add(new UniqueKey(stat.getTimestamp(), stat.getAccountId()));
        if (!added) {
          persistence.delete(stat);
          log.info("Deleted: {}", stat.getUuid());
          deleteCount++;
        }
      }

      log.info("Finished RemoveDupInstanceStats Migration. Deleted entries: {}", deleteCount);

    } catch (Exception e) {
      log.error("Error running RemoveDupInstanceStats migration. ", e);
    }
  }
}
