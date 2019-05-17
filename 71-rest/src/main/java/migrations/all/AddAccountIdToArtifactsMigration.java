package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class AddAccountIdToArtifactsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "artifacts");
    logger.info("Adding accountId to Artifacts");
    try (HIterator<Application> applicationHIterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (applicationHIterator.hasNext()) {
        Application application = applicationHIterator.next();
        logger.info("Adding accountId to artifacts for application {}", application.getUuid());
        final WriteResult result = collection.updateMulti(
            new BasicDBObject(ArtifactKeys.appId, application.getUuid()).append(ArtifactKeys.accountId, null),
            new BasicDBObject("$set", new BasicDBObject(ArtifactKeys.accountId, application.getAccountId())));
        logger.info("updated {} artifacts for application {} ", result.getN(), application.getUuid());
      }
    }
    logger.info("Adding accountIds to Artifacts completed for all applications");
  }
}
