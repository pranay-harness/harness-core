/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * There are some duplicate entries in
 * software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo#perpetualTaskIds field. This migration will
 * correct that anomaly
 */
@Slf4j
public class InstanceSyncPerpetualTaskInfoMigration implements Migration {
  private static final String DEBUG_LINE = "InstanceSyncPerpetualTaskInfoMigration:";
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<InstanceSyncPerpetualTaskInfo> it = new HIterator<>(
             wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class, HQuery.excludeAuthority).fetch())) {
      while (it.hasNext()) {
        final InstanceSyncPerpetualTaskInfo next = it.next();
        try {
          final List<String> perpetualTaskIds = next.getPerpetualTaskIds();
          if (isNotEmpty(perpetualTaskIds) && perpetualTaskIds.size() > 1) {
            final List<String> distinctPerpetualTaskIds = perpetualTaskIds.stream().distinct().collect(toList());
            if (distinctPerpetualTaskIds.size() < perpetualTaskIds.size()) {
              wingsPersistence.updateField(InstanceSyncPerpetualTaskInfo.class, next.getUuid(),
                  InstanceSyncPerpetualTaskInfoKeys.perpetualTaskIds, distinctPerpetualTaskIds);
            }
            logInfo(format("InstanceSyncPerpetualTaskInfo %s beforeSize: %d afterSize: %d", next.getUuid(),
                perpetualTaskIds.size(), distinctPerpetualTaskIds.size()));
          }
        } catch (Exception e) {
          logError(format("Error InstanceSyncPerpetualTaskInfo %s", next.getUuid()), e);
        }
      }
    }
  }

  private void logError(String str, Throwable t) {
    log.error(DEBUG_LINE + SPACE + str, t);
  }

  private void logInfo(String str) {
    log.info(DEBUG_LINE + SPACE + str);
  }
}
