package io.harness.generator;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.User.Builder.anUser;

import com.google.common.collect.ImmutableMap;
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
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
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
@Slf4j
public class AccountGenerator {
  private static final String adminUserUuid = "lv0euRhKRCyiXWzS7pOg6g";
  private static final String adminUserName = "Admin";
  public static final String adminUserEmail = "admin@harness.io";
  private static final SecretName adminPassword = new SecretName("user_admin_password");

  private static final String readOnlyUserUuid = "nhLgdGgxS_iqa0KP5edC-w";
  private static final String readOnlyUserName = "readonlyuser";
  public static final String readOnlyEmail = "readonlyuser@harness.io";
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

  private static final String testUserUuid = "12345-123456789012-123";
  private static final String testUserName = "testadmin";
  private static final String testEmail = "testadmin@harness.io";
  private static final SecretName testPassword = new SecretName("user_default_password");

  @Inject AccountService accountService;
  @Inject AuthHandler authHandler;
  @Inject HarnessUserGroupService harnessUserGroupService;
  @Inject LicenseGenerator licenseGenerator;
  @Inject ScmSecret scmSecret;
  @Inject UserGroupService userGroupService;
  @Inject UserService userService;
  @Inject WingsPersistence wingsPersistence;
  @Inject private LimitConfigurationService limitConfigurationService;

  public static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  public static final String TEST_ACCOUNT_ID = "1234567890123456789012";

  @Setter Account account;

  public enum Accounts { GENERIC_TEST, HARNESS_TEST }

  public Account ensurePredefined(Randomizer.Seed seed, Owners owners, Accounts predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest();
      case HARNESS_TEST:
        return ensureHarnessTest();
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Account exists(Account account) {
    return wingsPersistence.createQuery(Account.class).filter(AccountKeys.accountName, account.getAccountName()).get();
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

    this.setLicenseInfo(accountObj);

    try {
      this.account = accountService.save(accountObj);
      this.setAccountKey("harness_account_secret", this.account);

      this.account = accountService.get(accountId);
      this.setLimitConfiguration(accountId);
      ensureDefaultUsers(account);
    } catch (WingsException we) {
      // TODO: fix this Hack here.
    }

    return this.account;
  }

  /*
   * This function has to be cleaned up to provide only the
   * necessary entities. This gonna evolve over the time and hence
   * we are not reusing `ensureGenericTest`.
   */
  public Account ensureHarnessTest() {
    Account account = this.getOrCreateAccount(TEST_ACCOUNT_ID, "Harness Test", "Harness");

    try {
      this.setLicenseInfo(account);

      account = accountService.save(account);

      this.setAccountKey("harness_account_secret", account);
      this.setLimitConfiguration(account.getUuid());

      ensureTestUser(account);
    } catch (WingsException wEx) {
      logger.error(wEx.getMessage());
    }

    return account;
  }

  private Account getOrCreateAccount(String accountId, String accountName, String companyName) {
    Account account = anAccount().withAccountName(accountName).withCompanyName(companyName).build();

    account = exists(account);

    if (account == null) {
      logger.info("Account does not exist with accountName = {}", accountName);
      account = Account.Builder.anAccount()
                    .withUuid(accountId)
                    .withAccountName(accountName)
                    .withCompanyName(companyName)
                    .withLicenseInfo(LicenseInfo.builder()
                                         .accountType(AccountType.PAID)
                                         .accountStatus(AccountStatus.ACTIVE)
                                         .expiryTime(-1)
                                         .build())
                    .build();
    }

    return account;
  }

  private void setLicenseInfo(Account account) {
    final Seed seed = new Seed(0);
    licenseGenerator.ensurePredefined(seed, Licenses.TRIAL);

    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(Constants.DEFAULT_PAID_LICENSE_UNITS);
    account.setLicenseInfo(licenseInfo);
  }

  private void setAccountKey(String secretName, Account account) {
    // Update account key to make it work with delegate
    UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
    accountUpdateOperations.set("accountKey", scmSecret.decryptToString(new SecretName(secretName)));
    wingsPersistence.update(
        wingsPersistence.createQuery(Account.class).filter(AccountKeys.accountName, account.getAccountName()),
        accountUpdateOperations);
  }

  private void setLimitConfiguration(String accountId) {
    limitConfigurationService.configure(accountId, ActionType.CREATE_PIPELINE, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.CREATE_USER, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.CREATE_APPLICATION, new StaticLimit(1000));
    limitConfigurationService.configure(accountId, ActionType.DEPLOY, new RateLimit(1000, 1, TimeUnit.HOURS));
  }

  public Account ensureDefaultUsers(Account account) {
    UpdateOperations<Role> roleUpdateOperations = wingsPersistence.createUpdateOperations(Role.class);
    roleUpdateOperations.set("accountId", ACCOUNT_ID);
    wingsPersistence.update(
        wingsPersistence.createQuery(Role.class).filter(Role.ACCOUNT_ID_KEY, account.getUuid()), roleUpdateOperations);

    User adminUser =
        ensureUser(adminUserUuid, adminUserName, adminUserEmail, scmSecret.decryptToCharArray(adminPassword), account);
    ensureUser(defaultUserUuid, defaultUserName, defaultEmail, scmSecret.decryptToCharArray(defaultPassword), account);
    User readOnlyUser = ensureUser(
        readOnlyUserUuid, readOnlyUserName, readOnlyEmail, scmSecret.decryptToCharArray(readOnlyPassword), account);
    ensureUser(rbac1UserUuid, rbac1UserName, rbac1Email, scmSecret.decryptToCharArray(rbac1Password), account);
    ensureUser(rbac2UserUuid, rbac2UserName, rbac2Email, scmSecret.decryptToCharArray(rbac2Password), account);
    addUserToUserGroup(adminUser, account.getUuid(), UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
    UserGroup readOnlyUserGroup = authHandler.buildReadOnlyUserGroup(
        account.getUuid(), readOnlyUser, UserGroup.DEFAULT_READ_ONLY_USER_GROUP_NAME);
    readOnlyUserGroup = wingsPersistence.saveAndGet(UserGroup.class, readOnlyUserGroup);

    addUserToUserGroup(readOnlyUser, readOnlyUserGroup);

    addUserToHarnessUserGroup(adminUser);

    return account;
  }

  public Account ensureTestUser(Account account) {
    User testUser =
        ensureUser(testUserUuid, testUserName, testEmail, scmSecret.decryptToCharArray(defaultPassword), account);
    addUserToUserGroup(testUser, account.getUuid(), UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);

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

  private User ensureUser(String uuid, String userName, String email, char[] password, Account account) {
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
                    .withAccountName(account.getAccountName())
                    .withCompanyName(account.getCompanyName())
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
}
