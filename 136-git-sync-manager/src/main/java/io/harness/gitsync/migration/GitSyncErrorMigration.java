package io.harness.gitsync.migration;

import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncErrorMigration implements NGMigration {
  private final MongoTemplate mongoTemplate;

  @Override
  public void migrate() {
    try {
      mongoTemplate.findAllAndRemove(new Query(), GitSyncError.class);
    } catch (Exception exception) {
      log.error("Error occurred while cleaning gitSyncErrorNG collection, dropping it");
      mongoTemplate.dropCollection(GitSyncError.class);
    }
  }
}
