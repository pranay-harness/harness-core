package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;

@Slf4j
public class AddAccountIdToLogAnalysisRecordsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "logAnalysisRecords");
    logger.info("Adding accountId to Log Analysis Records");
    try (HIterator<Application> applicationHIterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (applicationHIterator.hasNext()) {
        Application application = applicationHIterator.next();
        logger.info("Adding accountId to logAnalysisRecords for application {}", application.getUuid());
        final WriteResult result = collection.updateMulti(
            new BasicDBObject("appId", application.getUuid()).append(LogMLAnalysisRecordKeys.accountId, null),
            new BasicDBObject(
                "$set", new BasicDBObject(LogMLAnalysisRecordKeys.accountId, application.getAccountId())));
        logger.info("updated {} records for application {} ", result.getN(), application.getUuid());
      }
    }
    logger.info("Adding accountIds to Log Analysis records completed for all applications");
  }
}
