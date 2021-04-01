package io.harness.migrations.all;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
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
@TargetModule(HarnessModule._390_DB_MIGRATION)
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
