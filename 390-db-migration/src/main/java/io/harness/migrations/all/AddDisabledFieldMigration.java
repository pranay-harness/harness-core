package io.harness.migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class AddDisabledFieldMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<User> userHIterator = new HIterator<>(
             wingsPersistence.createQuery(User.class).field(UserKeys.disabled).doesNotExist().fetch())) {
      while (userHIterator.hasNext()) {
        User user = userHIterator.next();
        if (!user.getDisabled()) {
          UpdateOperations<User> operations = wingsPersistence.createUpdateOperations(User.class);
          setUnset(operations, UserKeys.disabled, false);
          wingsPersistence.update(user, operations);
        }
      }
    } catch (Exception e) {
      log.error("Could not run migration for users disabled field ", e);
    }
  }
}
