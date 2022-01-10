/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DeleteDelegateAlertsExceptDelegateDown implements Migration {
  @Inject private HPersistence persistence;

  private static final List<String> AlertsToDelete = Arrays.asList("NoActiveDelegates", "NoInstalledDelegates",
      "DelegatesScalingGroupDownAlert", "DelegateProfileError", "NoEligibleDelegates", "PerpetualTaskAlert");

  @Override
  public void migrate() {
    log.info("Starting the migration for deleting delegate alerts except DELEGATE_DOWN alert");
    List<String> idsToDelete = new ArrayList<>();

    int deleted = 0;
    try (HIterator<Alert> iterator =
             new HIterator<>(persistence.createQuery(Alert.class).field(AlertKeys.type).in(AlertsToDelete).fetch())) {
      while (iterator.hasNext()) {
        idsToDelete.add(iterator.next().getUuid());

        deleted++;
        if (deleted != 0 && idsToDelete.size() % 500 == 0) {
          persistence.delete(persistence.createQuery(Alert.class).field(AlertKeys.uuid).in(idsToDelete));
          log.info("deleted: " + deleted);
          idsToDelete.clear();
        }
      }

      if (!idsToDelete.isEmpty()) {
        persistence.delete(persistence.createQuery(Alert.class).field(AlertKeys.uuid).in(idsToDelete));
        log.info("deleted: " + deleted);
      }
    }
    log.info(
        "Migration complete for deleting delegate alerts except DELEGATE_DOWN alert. Deleted " + deleted + " records.");
  }
}
