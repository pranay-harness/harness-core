/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.alert.entities.AlertRuleAnomaly.AlertRuleAnomalyKeys;
import io.harness.cvng.alert.entities.AlertRuleAnomaly.AlertRuleAnomalyStatus;
import io.harness.cvng.alert.services.AlertRuleAnomalyService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class AlertRuleAnomalyServiceImpl implements AlertRuleAnomalyService {
  @Inject private HPersistence hPersistence;
  @Inject private Clock clock;

  @Override
  public void closeAnomaly(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, CVMonitoringCategory category) {
    UpdateOperations<AlertRuleAnomaly> updateOperations = hPersistence.createUpdateOperations(AlertRuleAnomaly.class);

    Query<AlertRuleAnomaly> query =
        hPersistence.createQuery(AlertRuleAnomaly.class, excludeAuthority)
            .filter(AlertRuleAnomalyKeys.accountId, accountId)
            .filter(AlertRuleAnomalyKeys.orgIdentifier, orgIdentifier)
            .filter(AlertRuleAnomalyKeys.projectIdentifier, projectIdentifier)
            .filter(AlertRuleAnomalyKeys.serviceIdentifier, serviceIdentifier)
            .filter(AlertRuleAnomalyKeys.envIdentifier, envIdentifier)
            .filter(AlertRuleAnomalyKeys.category, category)
            .filter(AlertRuleAnomalyKeys.alertRuleAnomalyStatus, AlertRuleAnomalyStatus.OPEN);

    updateOperations.set(AlertRuleAnomalyKeys.alertRuleAnomalyStatus, AlertRuleAnomalyStatus.CLOSED);

    hPersistence.findAndModify(query, updateOperations, returnNewOptions);
  }

  @Override
  public void updateLastNotificationSentAt(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, CVMonitoringCategory category) {
    UpdateOperations<AlertRuleAnomaly> updateOperations = hPersistence.createUpdateOperations(AlertRuleAnomaly.class);

    Query<AlertRuleAnomaly> query =
        hPersistence.createQuery(AlertRuleAnomaly.class, excludeAuthority)
            .filter(AlertRuleAnomalyKeys.accountId, accountId)
            .filter(AlertRuleAnomalyKeys.orgIdentifier, orgIdentifier)
            .filter(AlertRuleAnomalyKeys.projectIdentifier, projectIdentifier)
            .filter(AlertRuleAnomalyKeys.serviceIdentifier, serviceIdentifier)
            .filter(AlertRuleAnomalyKeys.envIdentifier, envIdentifier)
            .filter(AlertRuleAnomalyKeys.category, category)
            .filter(AlertRuleAnomalyKeys.alertRuleAnomalyStatus, AlertRuleAnomalyStatus.OPEN);

    updateOperations.set(AlertRuleAnomalyKeys.lastNotificationSentAt, clock.instant());

    hPersistence.findAndModify(query, updateOperations, returnNewOptions);
  }

  @Override
  public AlertRuleAnomaly openAnomaly(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, CVMonitoringCategory category) {
    Query<AlertRuleAnomaly> query =
        hPersistence.createQuery(AlertRuleAnomaly.class, excludeAuthority)
            .filter(AlertRuleAnomalyKeys.accountId, accountId)
            .filter(AlertRuleAnomalyKeys.orgIdentifier, orgIdentifier)
            .filter(AlertRuleAnomalyKeys.projectIdentifier, projectIdentifier)
            .filter(AlertRuleAnomalyKeys.serviceIdentifier, serviceIdentifier)
            .filter(AlertRuleAnomalyKeys.envIdentifier, envIdentifier)
            .filter(AlertRuleAnomalyKeys.category, category)
            .filter(AlertRuleAnomalyKeys.alertRuleAnomalyStatus, AlertRuleAnomalyStatus.OPEN);

    return hPersistence.upsert(query,
        hPersistence.createUpdateOperations(AlertRuleAnomaly.class)
            .setOnInsert(AlertRuleAnomalyKeys.accountId, accountId)
            .setOnInsert(AlertRuleAnomalyKeys.uuid, generateUuid())
            .setOnInsert(AlertRuleAnomalyKeys.orgIdentifier, orgIdentifier)
            .setOnInsert(AlertRuleAnomalyKeys.projectIdentifier, projectIdentifier)
            .setOnInsert(AlertRuleAnomalyKeys.serviceIdentifier, serviceIdentifier)
            .setOnInsert(AlertRuleAnomalyKeys.envIdentifier, envIdentifier)
            .setOnInsert(AlertRuleAnomalyKeys.category, category)
            .setOnInsert(AlertRuleAnomalyKeys.alertRuleAnomalyStatus, AlertRuleAnomalyStatus.OPEN),
        new FindAndModifyOptions().upsert(true));
  }
}
