package io.harness.generator;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.Account.ACCOUNT_NAME_KEY;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.generator.LicenseGenerator.Licenses;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.Setter;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.beans.security.UserGroup;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

@Singleton
public class AccountGenerator {
  private static final String adminUserName = "Admin";
  private static final String adminUserEmail = "admin@harness.io";
  private static final SecretName adminPassword = new SecretName("user_admin_password");

  private static final String readOnlyUserName = "readonlyuser";
  private static final String readOnlyEmail = "readonlyuser@harness.io";
  private static final SecretName readOnlyPassword = new SecretName("user_readonly_password");

  private static final String rbac1UserName = "rbac1";
  private static final String rbac1Email = "rbac1@harness.io";
  private static final SecretName rbac1Password = new SecretName("user_rbac1_password");

  private static final String rbac2UserName = "rbac2";
  private static final String rbac2Email = "rbac2@harness.io";
  private static final SecretName rbac2Password = new SecretName("user_rbac2_password");

  private static final String defaultUserName = "default";
  private static final String defaultEmail = "default@harness.io";
  private static final SecretName defaultPassword = new SecretName("user_default_password");

  @Inject AccountService accountService;
  @Inject AuthHandler authHandler;
  @Inject HarnessUserGroupService harnessUserGroupService;
  @Inject LicenseGenerator licenseGenerator;
  @Inject ScmSecret scmSecret;
  @Inject UserGroupService userGroupService;
  @Inject UserService userService;
  @Inject WingsPersistence wingsPersistence;

  @Setter Account account;

  public enum Accounts {
    GENERIC_TEST,
  }

  public Account ensurePredefined(Randomizer.Seed seed, Owners owners, Accounts predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest();
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Account exists(Account account) {
    return wingsPersistence.createQuery(Account.class).filter(ACCOUNT_NAME_KEY, account.getAccountName()).get();
  }

  public Account ensureGenericTest() {
    String accountId = "kmpySmUISimoRrJL6NL73w";

    if (this.account != null) {
      return this.account;
    }

    Account accountObj = anAccount().withAccountName("Harness").withCompanyName("Harness").build();
    this.account = exists(accountObj);
    if (this.account != null) {
      return account;
    }
    accountObj = Account.Builder.anAccount()
                     .withUuid(accountId)
                     .withAccountName("Harness")
                     .withCompanyName("Harness")
                     .withLicenseInfo(LicenseInfo.builder()
                                          .accountType(AccountType.PAID)
                                          .accountStatus(AccountStatus.ACTIVE)
                                          .expiryTime(-1)
                                          .build())
                     .build();

    final Seed seed = new Seed(0);
    licenseGenerator.ensurePredefined(seed, Licenses.TRIAL);

    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(Constants.DEFAULT_PAID_LICENSE_UNITS);
    accountObj.setLicenseInfo(licenseInfo);

    accountService.save(accountObj);

    // Update account key to make it work with delegate
    UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
    accountUpdateOperations.set("accountKey", scmSecret.decryptToString(new SecretName("harness_account_secret")));
    wingsPersistence.update(wingsPersistence.createQuery(Account.class), accountUpdateOperations);

    this.account = accountService.get(accountId);
    ensureDefaultUsers(account);

    return this.account;
  }

  public Account ensureDefaultUsers(Account account) {
    UpdateOperations<User> userUpdateOperations = wingsPersistence.createUpdateOperations(User.class);
    userUpdateOperations.set("accounts", Lists.newArrayList(account));
    wingsPersistence.update(wingsPersistence.createQuery(User.class), userUpdateOperations);

    UpdateOperations<Role> roleUpdateOperations = wingsPersistence.createUpdateOperations(Role.class);
    roleUpdateOperations.set("accountId", "kmpySmUISimoRrJL6NL73w");
    wingsPersistence.update(wingsPersistence.createQuery(Role.class), roleUpdateOperations);

    User adminUser = addUser(adminUserName, adminUserEmail, scmSecret.decryptToCharArray(adminPassword), account);
    addUser(defaultUserName, defaultEmail, scmSecret.decryptToCharArray(defaultPassword), account);
    User readOnlyUser =
        addUser(readOnlyUserName, readOnlyEmail, scmSecret.decryptToCharArray(readOnlyPassword), account);
    addUser(rbac1UserName, rbac1Email, scmSecret.decryptToCharArray(rbac1Password), account);
    addUser(rbac2UserName, rbac2Email, scmSecret.decryptToCharArray(rbac2Password), account);
    addUserToUserGroup(adminUser, account.getUuid(), Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
    UserGroup readOnlyUserGroup = authHandler.buildReadOnlyUserGroup(
        account.getUuid(), readOnlyUser, Constants.DEFAULT_READ_ONLY_USER_GROUP_NAME);
    readOnlyUserGroup = wingsPersistence.saveAndGet(UserGroup.class, readOnlyUserGroup);

    addUserToUserGroup(readOnlyUser, readOnlyUserGroup);

    addUserToHarnessUserGroup(adminUser);

    //    loginAdminUser();

    return account;
  }

  private void addUserToUserGroup(User user, String accountId, String userGroupName) {
    PageRequest<UserGroup> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("name", EQ, userGroupName).build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    UserGroup userGroup = pageResponse.get(0);
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup, false);
  }

  private void addUserToUserGroup(User user, UserGroup userGroup) {
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup, false);
  }

  private void addUserToHarnessUserGroup(User user) {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(user.getUuid()))
                                            .name("harnessUserGroup")
                                            .build();
    harnessUserGroupService.save(harnessUserGroup);
  }

  private User addUser(String userName, String email, char[] password, Account account) {
    User user = anUser()
                    .withName(userName)
                    .withEmail(email)
                    .withPassword(password)
                    .withRoles(wingsPersistence
                                   .query(Role.class,
                                       aPageRequest()
                                           .addFilter(ACCOUNT_ID_KEY, EQ, account.getUuid())
                                           .addFilter("roleType", EQ, RoleType.ACCOUNT_ADMIN)
                                           .build())
                                   .getResponse())
                    .withAccountName(HARNESS_NAME)
                    .withCompanyName(HARNESS_NAME)
                    .build();
    User newUser = userService.registerNewUser(user, account);
    wingsPersistence.updateFields(User.class, newUser.getUuid(), ImmutableMap.of("emailVerified", true));

    return wingsPersistence.get(User.class, newUser.getUuid());
  }

  public Account ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Accounts predefined = random.nextObject(Accounts.class);
    return ensurePredefined(seed, owners, predefined);
  }

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}
