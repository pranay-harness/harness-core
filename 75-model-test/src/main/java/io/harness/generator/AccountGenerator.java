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
import io.harness.exception.WingsException;
import io.harness.generator.LicenseGenerator.Licenses;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.Setter;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.concurrent.TimeUnit;

@Singleton
public class AccountGenerator {
  private static final String adminUserUuid = "lv0euRhKRCyiXWzS7pOg6g";
  private static final String adminUserName = "Admin";
  private static final String adminUserEmail = "admin@harness.io";
  private static final SecretName adminPassword = new SecretName("user_admin_password");

  private static final String readOnlyUserUuid = "nhLgdGgxS_iqa0KP5edC-w";
  private static final String readOnlyUserName = "readonlyuser";
  private static final String readOnlyEmail = "readonlyuser@harness.io";
  private static final SecretName readOnlyPassword = new SecretName("user_readonly_password");

  private static final String rbac1UserUuid = "BnTbQTIJS4SkadzYv0BcbA";
  private static final String rbac1UserName = "rbac1";
  private static final String rbac1Email = "rbac1@harness.io";
  private static final SecretName rbac1Password = new SecretName("user_rbac1_password");

  private static final String rbac2UserUuid = "19bYA-ooQZOTZQxf2N-VPA";
  private static final String rbac2UserName = "rbac2";
  private static final String rbac2Email = "rbac2@harness.io";
  private static final SecretName rbac2Password = new SecretName("user_rbac2_password");

  private static final String defaultUserUuid = "0osgWsTZRsSZ8RWfjLRkEg";
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
  @Inject private LimitConfigurationService limitConfigurationService;

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

  @Setter Account account;
  private static final Logger logger = LoggerFactory.getLogger(AccountGenerator.class);

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
    String accountId = ACCOUNT_ID;
    Account accountObj = anAccount().withAccountName("Harness").withCompanyName("Harness").build();
    this.account = exists(accountObj);
    if (this.account == null) {
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
    }
    final Seed seed = new Seed(0);
    licenseGenerator.ensurePredefined(seed, Licenses.TRIAL);

    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(Constants.DEFAULT_PAID_LICENSE_UNITS);
    accountObj.setLicenseInfo(licenseInfo);

    try {
      accountService.save(accountObj);
    } catch (WingsException we) {
      // TODO: fix this Hack here.
    }

    // Update account key to make it work with delegate
    UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
    accountUpdateOperations.set("accountKey", scmSecret.decryptToString(new SecretName("harness_account_secret")));
    wingsPersistence.update(wingsPersistence.createQuery(Account.class), accountUpdateOperations);

    this.account = accountService.get(accountId);
    ensureDefaultUsers(account);

    limitConfigurationService.configure(accountId, ActionType.CREATE_PIPELINE, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.CREATE_USER, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.CREATE_APPLICATION, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.DEPLOY, new RateLimit(1000, 1, TimeUnit.HOURS));

    return this.account;
  }

  public Account ensureDefaultUsers(Account account) {
    UpdateOperations<User> userUpdateOperations = wingsPersistence.createUpdateOperations(User.class);
    userUpdateOperations.set("accounts", Lists.newArrayList(account));
    wingsPersistence.update(wingsPersistence.createQuery(User.class), userUpdateOperations);

    UpdateOperations<Role> roleUpdateOperations = wingsPersistence.createUpdateOperations(Role.class);
    roleUpdateOperations.set("accountId", ACCOUNT_ID);
    wingsPersistence.update(wingsPersistence.createQuery(Role.class), roleUpdateOperations);

    User adminUser =
        addUser(adminUserUuid, adminUserName, adminUserEmail, scmSecret.decryptToCharArray(adminPassword), account);
    addUser(defaultUserUuid, defaultUserName, defaultEmail, scmSecret.decryptToCharArray(defaultPassword), account);
    User readOnlyUser = addUser(
        readOnlyUserUuid, readOnlyUserName, readOnlyEmail, scmSecret.decryptToCharArray(readOnlyPassword), account);
    addUser(rbac1UserUuid, rbac1UserName, rbac1Email, scmSecret.decryptToCharArray(rbac1Password), account);
    addUser(rbac2UserUuid, rbac2UserName, rbac2Email, scmSecret.decryptToCharArray(rbac2Password), account);
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

  private User addUser(String uuid, String userName, String email, char[] password, Account account) {
    User user = anUser()
                    .withUuid(uuid)
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
