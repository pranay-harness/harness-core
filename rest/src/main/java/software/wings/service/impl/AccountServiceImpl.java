package software.wings.service.impl;

import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.ACCOUNT_ADMIN;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.AppContainer;
import software.wings.beans.ErrorCode;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SystemCatalog;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.licensing.LicenseManager;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Singleton
@ValidateOnExecution
public class AccountServiceImpl implements AccountService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private RoleService roleService;
  @Inject private LicenseManager licenseManager;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private SettingsService settingsService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;
  @Inject private AppContainerService appContainerService;
  @Inject private SystemCatalogService systemCatalogService;

  @Override
  public Account save(@Valid Account account) {
    account.setAccountKey(generateAccountKey());
    // licenseManager.setLicense(account);
    wingsPersistence.save(account);
    createDefaultAccountEntites(account);
    return account;
  }

  private void createDefaultAccountEntites(Account account) {
    settingsService.createDefaultAccountSettings(account.getUuid());
    createDefaultRoles(account)
        .stream()
        .filter(role -> RoleType.ACCOUNT_ADMIN.equals(role.getRoleType()))
        .forEach(role -> createDefaultNotificationGroup(account, role));
    createSystemAppContainers(account);
  }

  List<Role> createDefaultRoles(Account account) {
    return Lists.newArrayList(roleService.save(aRole()
                                                   .withAppId(GLOBAL_APP_ID)
                                                   .withAccountId(account.getUuid())
                                                   .withName(ACCOUNT_ADMIN.getDisplayName())
                                                   .withRoleType(ACCOUNT_ADMIN)
                                                   .build()),

        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(APPLICATION_ADMIN.getDisplayName())
                             .withRoleType(APPLICATION_ADMIN)
                             .withAllApps(true)
                             .build()),
        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(PROD_SUPPORT.getDisplayName())
                             .withRoleType(PROD_SUPPORT)
                             .withAllApps(true)
                             .build()),
        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(NON_PROD_SUPPORT.getDisplayName())
                             .withRoleType(NON_PROD_SUPPORT)
                             .withAllApps(true)
                             .build()));
  }

  @Override
  public Account get(String accountId) {
    return wingsPersistence.get(Account.class, accountId);
  }

  @Override
  public void delete(String accountId) {
    boolean deleted = wingsPersistence.delete(Account.class, accountId);
    if (deleted) {
      executorService.submit(() -> {
        settingsService.deleteByAccountId(accountId);
        appService.deleteByAccountId(accountId);
      });
    }
  }

  //  @Override
  //  public Account findOrCreate(String companyName) {
  //    return
  //    wingsPersistence.upsert(wingsPersistence.createQuery(Account.class).field("companyName").equal(companyName),
  //        wingsPersistence.createUpdateOperations(Account.class).setOnInsert("companyName",
  //        companyName).setOnInsert("accountKey", generateAccountKey()));
  //  }

  @Override
  public String suggestAccountName(String accountName) {
    String suggestedAccountName = accountName;
    Random rand = new Random();
    int i = rand.nextInt(1000);
    do {
      Account res = wingsPersistence.get(
          Account.class, Builder.aPageRequest().addFilter("accountName", Operator.EQ, suggestedAccountName).build());
      if (res == null) {
        return suggestedAccountName;
      }
      suggestedAccountName = accountName + rand.nextInt(1000);
    } while (true);
  }

  @Override
  public boolean exists(String accountName) {
    Account res = wingsPersistence.get(
        Account.class, Builder.aPageRequest().addFilter("accountName", Operator.EQ, accountName).build());
    return (res != null);
  }

  @Override
  public Account update(@Valid Account account) {
    wingsPersistence.update(
        account, wingsPersistence.createUpdateOperations(Account.class).set("companyName", account.getCompanyName()));
    return wingsPersistence.get(Account.class, account.getUuid());
  }

  @Override
  public Account getByName(String companyName) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(Account.class).field("companyName").equal(companyName));
  }

  @Override
  public List<Account> list(PageRequest<Account> pageRequest) {
    return wingsPersistence.query(Account.class, pageRequest).getResponse();
  }
  private void createDefaultNotificationGroup(Account account, Role role) {
    String name = role.getRoleType().getDisplayName();
    // check if the notification group name exists
    List<NotificationGroup> existingGroups =
        notificationSetupService.listNotificationGroups(account.getUuid(), role, name);
    if (existingGroups == null || existingGroups.isEmpty()) {
      logger.info("Creating default {} notification group {} for account {}", ACCOUNT_ADMIN.getDisplayName(), name,
          account.getAccountName());
      NotificationGroup notificationGroup = aNotificationGroup()
                                                .withAppId(account.getAppId())
                                                .withAccountId(account.getUuid())
                                                .withRole(role)
                                                .withName(name)
                                                .withEditable(false)
                                                .build();
      notificationSetupService.createNotificationGroup(notificationGroup);
    } else {
      logger.info("Default notification group already exists for role {} and account {}",
          ACCOUNT_ADMIN.getDisplayName(), account.getAccountName());
    }
  }

  private void createSystemAppContainers(Account account) {
    List<SystemCatalog> systemCatalogs =
        systemCatalogService.list(aPageRequest().addFilter("catalogType", EQ, APPSTACK).build());
    logger.debug("Creating default system app containers  ");
    for (SystemCatalog systemCatalog : systemCatalogs) {
      AppContainer appContainer = anAppContainer()
                                      .withAccountId(account.getUuid())
                                      .withAppId(systemCatalog.getAppId())
                                      .withChecksum(systemCatalog.getChecksum())
                                      .withChecksumType(systemCatalog.getChecksumType())
                                      .withFamily(systemCatalog.getFamily())
                                      .withStackRootDirectory(systemCatalog.getStackRootDirectory())
                                      .withFileName(systemCatalog.getFileName())
                                      .withFileUuid(systemCatalog.getFileUuid())
                                      .withFileType(systemCatalog.getFileType())
                                      .withSize(systemCatalog.getSize())
                                      .withName(systemCatalog.getName())
                                      .withSystemCreated(true)
                                      .withDescription(systemCatalog.getNotes())
                                      .withHardened(systemCatalog.isHardened())
                                      .withVersion(systemCatalog.getVersion())
                                      .build();
      try {
        appContainerService.save(appContainer);
      } catch (Exception e) {
        logger.warn("Error while creating system app container {}", appContainer);
      }
    }
  }

  private String generateAccountKey() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      logger.error("Exception while generating account key: " + e.getMessage(), e);
      for (StackTraceElement elem : e.getStackTrace()) {
        logger.error("Trace: {}", elem);
      }
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    return Hex.encodeHexString(encoded);
  }
}
