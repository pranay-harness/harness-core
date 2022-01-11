/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jobs.sg247.logs;

import io.harness.mongo.iterator.MongoPersistenceIterator;

import software.wings.beans.alert.Alert;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceGuardCleanUpAlertsJob implements MongoPersistenceIterator.Handler<Alert> {
  // TODO: Remove this iterator when the alerts with configurable TTL is available.:
  // https://harness.atlassian.net/browse/PL-8790
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void handle(Alert alert) {
    if (alert != null) {
      log.info("Deleting service guard alert with UUID: {}", alert.getUuid());
      wingsPersistence.delete(Alert.class, alert.getUuid());
    }
  }
}
