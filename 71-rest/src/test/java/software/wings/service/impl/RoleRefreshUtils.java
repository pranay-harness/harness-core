package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.RAGHU;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.util.List;

/**
 * Created by rishi on 3/14/17.
 */
@Integration
public class RoleRefreshUtils extends WingsBaseTest {
  @Inject UserService userService;
  @Inject RoleService roleService;
  @Inject AccountService accountService;
  @Inject WingsPersistence wingsPersistence;
  @Inject AppService appService;

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void recreateRoles() {
    PageResponse<User> users = userService.list(aPageRequest().build(), false);

    PageResponse<Account> accounts = wingsPersistence.query(Account.class, aPageRequest().build());

    dropRolesCollection();
    AccountServiceImpl accountServiceImpl = (AccountServiceImpl) accountService;
    accounts.forEach(account -> {
      List<Role> newRoles = accountServiceImpl.createDefaultRoles(account);
      Query<User> usersQuery = wingsPersistence.createQuery(User.class);
      UpdateOperations<User> usersUpdate =
          wingsPersistence.createUpdateOperations(User.class).set("roles", Lists.newArrayList(newRoles.get(0)));
      users.forEach(user -> { wingsPersistence.update(usersQuery, usersUpdate); });
    });

    AppServiceImpl appServiceImpl = (AppServiceImpl) appService;
    wingsPersistence.query(Application.class, aPageRequest().build()).forEach(app -> {
      appServiceImpl.createDefaultRoles((Application) app);
    });
  }

  private void dropRolesCollection() {
    wingsPersistence.getCollection(Role.class).drop();
  }
}
