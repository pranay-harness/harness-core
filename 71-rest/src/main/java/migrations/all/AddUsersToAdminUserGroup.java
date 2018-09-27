package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.FeatureName.RBAC;
import static software.wings.common.Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import migrations.Migration;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.List;
import java.util.Optional;

/**
 * Migration script to add existing users to admin user group for accounts
 * in which rbac is still disabled. Rbac will be enabled for all accounts after this script.
 * This script is meant to be idempotent, so it could be run any number of times.
 * @author rktummala on 5/09/18
 */
public class AddUsersToAdminUserGroup implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddUsersToAdminUserGroup.class);

  @Inject private AuthHandler authHandler;
  @Inject private UserService userService;
  @Inject private UserGroupService userGroupService;
  @Inject private FeatureFlagService featureFlagService;

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

        accounts.forEach(account -> {
          boolean rbacEnabled = featureFlagService.isEnabled(RBAC, account.getUuid());
          List<UserGroup> userGroupList = userGroupService.getUserGroupsByAccountId(account.getUuid(), user);
          if (!rbacEnabled && isEmpty(userGroupList)) {
            PageResponse<UserGroup> pageResponse =
                userGroupService.list(account.getUuid(), aPageRequest().build(), true);
            List<UserGroup> userGroupsOfAccount = pageResponse.getResponse();
            Optional<UserGroup> userGroupOptional =
                userGroupsOfAccount.stream()
                    .filter(userGroup -> userGroup.getName().equals(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME))
                    .findAny();
            if (userGroupOptional.isPresent()) {
              UserGroup userGroup = userGroupOptional.get();
              List<User> members = userGroup.getMembers();
              if (members == null) {
                members = Lists.newArrayList();
                userGroup.setMembers(members);
              }

              userGroup.getMembers().add(user);
              userGroupService.updateMembers(userGroup, false);
            } else {
              logger.error("No Account Admin User Group is found for account: {}", account.getUuid());
            }
          }
        });
      });
    }
  }
}
