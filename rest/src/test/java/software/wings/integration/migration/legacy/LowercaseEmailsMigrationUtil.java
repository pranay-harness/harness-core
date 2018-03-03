package software.wings.integration.migration.legacy;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * Migration script to make all user emails lowercase
 * @author brett on 10/13/17
 */
@Integration
@Ignore
public class LowercaseEmailsMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(LowercaseEmailsMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void lowercaseEmails() {
    logger.info("Checking emails");

    List<User> users = wingsPersistence.createQuery(User.class).asList();
    for (User user : users) {
      String lowercaseEmail = user.getEmail().trim().toLowerCase();
      if (!lowercaseEmail.equals(user.getEmail())) {
        logger.info(user.getEmail());
        wingsPersistence.updateField(User.class, user.getUuid(), "email", lowercaseEmail);
      }
    }
    logger.info("\nLowercase emails completed");
  }
}
