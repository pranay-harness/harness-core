package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.User;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class UnregisteredUserNameMigration implements Migration {
  public static final String NOT_REGISTERED = "<Not registered yet>";

  @Inject WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    log.info("Migrating unregistered usernames");

    Query<User> query = wingsPersistence.createQuery(User.class).field("name").equal(NOT_REGISTERED);
    log.info("Updating " + query.count() + " user entries");
    try (HIterator<User> userIterator = new HIterator<>(query.fetch())) {
      for (User user : userIterator) {
        wingsPersistence.update(user, wingsPersistence.createUpdateOperations(User.class).set("name", user.getEmail()));
      }
    }
  }
}
