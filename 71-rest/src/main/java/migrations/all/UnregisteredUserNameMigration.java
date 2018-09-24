package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;

import java.util.Iterator;

public class UnregisteredUserNameMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(UnregisteredUserNameMigration.class);

  @Inject WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    logger.info("Migrating unregistered usernames");

    Query<User> query = wingsPersistence.createQuery(User.class).field("name").equal(Constants.NOT_REGISTERED);
    logger.info("Updating " + query.count() + " user entries");
    Iterator<User> userIterator = query.iterator();
    while (userIterator.hasNext()) {
      User user = userIterator.next();
      wingsPersistence.update(user, wingsPersistence.createUpdateOperations(User.class).set("name", user.getEmail()));
    }
  }
}