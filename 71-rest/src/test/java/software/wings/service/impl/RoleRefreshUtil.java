package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.persistence.ReadPref;
import org.junit.Ignore;
import org.junit.Test;
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
@Ignore
public class RoleRefreshUtil extends WingsBaseTest {
  @Inject UserService userService;
  @Inject RoleService roleService;
  @Inject AccountService accountService;
  @Inject WingsPersistence wingsPersistence;
  @Inject AppService appService;

  @Test
  @Ignore
  public void recreateRoles() {
    PageResponse<User> users = userService.list(aPageRequest().build());

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
    wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).getDB().getCollection("roles").drop();
  }
}
