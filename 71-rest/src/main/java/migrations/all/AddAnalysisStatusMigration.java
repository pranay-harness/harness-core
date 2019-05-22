package migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;

import java.util.concurrent.TimeUnit;

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
    logger.info("Updated {} logMLAnalysisRecords with analysisStatus", results.getUpdatedCount());
  }
}
