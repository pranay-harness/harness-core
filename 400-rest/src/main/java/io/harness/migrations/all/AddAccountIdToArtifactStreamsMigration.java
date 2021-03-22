
package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToArtifactStreamsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "artifactStream");
    log.info("Starting migration - Adding accountId to Artifact Streams");
    try (HIterator<Application> applicationHIterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (applicationHIterator.hasNext()) {
        Application application = applicationHIterator.next();
        log.info("Adding accountId to artifact streams for application {}", application.getUuid());
        final WriteResult result =
            collection.updateMulti(new BasicDBObject(ArtifactStreamKeys.appId, application.getUuid())
                                       .append(ArtifactStreamKeys.accountId, null),
                new BasicDBObject("$set", new BasicDBObject(ArtifactStreamKeys.accountId, application.getAccountId())));
        log.info("Updated {} artifact streams for application {}", result.getN(), application.getUuid());
      }
    }
    log.info("Adding accountIds to Artifact Streams completed for all applications");
  }
}
