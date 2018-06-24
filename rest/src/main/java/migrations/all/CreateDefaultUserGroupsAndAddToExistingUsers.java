package migrations.all;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import migrations.Migration;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.UserService;

import java.util.List;

/**
 * Migration script to migrate all the existing user groups
 * This script is meant to be idempotent, so it could be run any number of times.
 * @author rktummala on 3/15/18
 */
public class CreateDefaultUserGroupsAndAddToExistingUsers implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(CreateDefaultUserGroupsAndAddToExistingUsers.class);

  @Inject private AuthHandler authHandler;
  @Inject private UserService userService;

  @Override
  public void migrate() {
    PageRequest<User> userPageRequest = aPageRequest().withLimit(UNLIMITED).build();
    PageResponse<User> userPageResponse = userService.list(userPageRequest);
    List<User> userList = userPageResponse.getResponse();

    if (userList != null) {
      userList.forEach(user -> {
        List<Account> accounts = user.getAccounts();
        if (CollectionUtils.isEmpty(accounts)) {
          logger.info("User {} is not associated to any account", user.getName());
          return;
        }

        accounts.forEach(account -> authHandler.addUserToDefaultAccountAdminUserGroup(user, account));
      });
    }
  }
}
