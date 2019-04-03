package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_PROD_SUPPORT_USER_GROUP_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserGroupService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Migration script to create default support user groups and rename account admin user group
 * This script is meant to be idempotent, so it could be run any number of times.
 * @author rktummala on 3/21/18
 */
public class CreateSupportUserGroupsAndRenameAccountAdmin implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(CreateSupportUserGroupsAndRenameAccountAdmin.class);

  private static final String DEFAULT_OLD_USER_GROUP_NAME = "ADMIN";

  @Inject private AuthHandler authHandler;
  @Inject private AccountService accountService;
  @Inject private UserGroupService userGroupService;

  @Override
  public void migrate() {
    PageRequest<Account> accountPageRequest = aPageRequest().withLimit(UNLIMITED).build();
    List<Account> accountList = accountService.list(accountPageRequest);

    if (accountList != null) {
      accountList.forEach(account -> {
        String accountId = account.getUuid();
        PageRequest<UserGroup> pageRequest =
            aPageRequest()
                .addFilter("accountId", EQ, accountId)
                .addFilter("name", IN, DEFAULT_OLD_USER_GROUP_NAME, DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME,
                    DEFAULT_PROD_SUPPORT_USER_GROUP_NAME, DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME)
                .build();
        PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);

        List<UserGroup> userGroupList = pageResponse.getResponse();

        Set<UserGroup> userGroupSet = new HashSet<>(userGroupList);

        if (!isUserGroupPresent(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME, userGroupSet)) {
          UserGroup userGroup = getUserGroup(DEFAULT_OLD_USER_GROUP_NAME, userGroupSet);
          if (userGroup != null) {
            userGroup.setName(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
            userGroupService.updateOverview(userGroup);
          } else {
            UserGroup defaultAdminUserGroup = authHandler.buildDefaultAdminUserGroup(accountId, null);
            userGroupService.save(defaultAdminUserGroup);
          }
        }

        if (!isUserGroupPresent(DEFAULT_PROD_SUPPORT_USER_GROUP_NAME, userGroupSet)) {
          UserGroup prodSupportUserGroup = authHandler.buildProdSupportUserGroup(accountId);
          userGroupService.save(prodSupportUserGroup);
        }

        if (!isUserGroupPresent(DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME, userGroupSet)) {
          UserGroup nonProdSupportUserGroup = authHandler.buildNonProdSupportUserGroup(accountId);
          userGroupService.save(nonProdSupportUserGroup);
        }
      });
    }
  }

  private UserGroup getUserGroup(String userGroupName, Set<UserGroup> userGroupSet) {
    Optional<UserGroup> userGroupOptional =
        userGroupSet.stream().filter(userGroup -> userGroupName.equals(userGroup.getName())).findFirst();
    if (userGroupOptional.isPresent()) {
      return userGroupOptional.get();
    }
    return null;
  }

  private boolean isUserGroupPresent(String userGroupName, Set<UserGroup> userGroupSet) {
    return userGroupSet.stream().filter(userGroup -> userGroupName.equals(userGroup.getName())).findFirst().isPresent();
  }
}
