/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import io.harness.migrations.Migration;
import io.harness.time.Timestamp;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class AddAnalysisStatusMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(7);

    Query<LogMLAnalysisRecord> logDataRecordQuery = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                        .field("createdAt")
                                                        .greaterThan(startTime)
                                                        .field(LogMLAnalysisRecordKeys.analysisStatus)
                                                        .doesNotExist();

    UpdateOperations<LogMLAnalysisRecord> op = wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class);
    setUnset(op, LogMLAnalysisRecordKeys.analysisStatus, LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);
    UpdateResults results = wingsPersistence.update(logDataRecordQuery, op);
    log.info("Updated {} logMLAnalysisRecords with analysisStatus", results.getUpdatedCount());
  }
}
