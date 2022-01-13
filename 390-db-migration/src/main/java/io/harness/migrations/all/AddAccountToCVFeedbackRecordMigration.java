/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;
import software.wings.sm.StateExecutionInstance;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddAccountToCVFeedbackRecordMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).build();

    List<CVFeedbackRecord> records = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);
    records.forEach(record -> {
      try {
        if (isNotEmpty(records)) {
          String cvConfig = record.getCvConfigId();
          String stateExecId = record.getStateExecutionId();
          if (isNotEmpty(cvConfig)) {
            CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, cvConfig);
            if (cvConfiguration != null) {
              record.setAccountId(cvConfiguration.getAccountId());
            } else {
              log.info("Bad cvConfigID found in CVFeedbackRecord" + cvConfig);
            }
          } else {
            StateExecutionInstance stateExecutionInstance =
                wingsPersistence.get(StateExecutionInstance.class, stateExecId);
            String accountId = appService.getAccountIdByAppId(stateExecutionInstance.getAppId());
            record.setAccountId(accountId);
          }
        }

      } catch (Exception ex) {
        log.info("Failure while adding accountId to CVFeedbackRecord");
      }
    });

    dataStoreService.save(CVFeedbackRecord.class, records, true);
  }
}
