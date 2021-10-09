package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

/**
 * Previously user invites are not deleted if the corresponding user with the same email address has been deleted.
 * This migration is to clean up all user invites that their corresponding users no longer exist in the user collection.
 * @author marklu on 2018-12-09
 */
@Slf4j
public class DanglingUserInviteCleanupMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;

  @Override
  public void migrate() {
    Query<UserInvite> userInviteQuery = wingsPersistence.createQuery(UserInvite.class, excludeAuthority);
    try (HIterator<UserInvite> records = new HIterator<>(userInviteQuery.fetch())) {
      while (records.hasNext()) {
        UserInvite userInvite = records.next();
        String email = userInvite.getEmail();
        User user = userService.getUserByEmail(email);
        if (user == null) {
          // User has been deleted already. Remove corresponding user invites!
          wingsPersistence.delete(userInvite);
          log.info("User '{}' has been deleted. Deleting it's corresponding user invite.", email);
        }
      }
    }
  }
}
