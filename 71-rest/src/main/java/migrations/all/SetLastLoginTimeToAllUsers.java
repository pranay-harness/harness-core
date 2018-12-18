package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserService;

/**
 * Migration script to set last login time for all existing users.
 * Going forward, the correct last login time would be set.
 * @author rktummala on 12/18/18
 */
public class SetLastLoginTimeToAllUsers implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SetLastLoginTimeToAllUsers.class);

  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Start - Setting user last login time for users");
    long currentTime = System.currentTimeMillis();
    try (HIterator<User> users = new HIterator<>(wingsPersistence.createQuery(User.class).fetch())) {
      while (users.hasNext()) {
        User user = null;
        try {
          user = users.next();
          user.setLastLogin(currentTime);
          userService.update(user);
        } catch (Exception ex) {
          logger.error("Error while setting user last login for user {}", user == null ? "NA" : user.getName(), ex);
        }
      }
    }

    logger.info("End - Setting user last login time for users");
  }
}
