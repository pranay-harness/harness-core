package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static software.wings.dl.exportimport.WingsMongoExportImport.getCollectionName;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.harness.annotation.HarnessEntity;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import io.harness.rest.RestResponse;
import io.harness.scheduler.PersistentScheduler;
import io.harness.security.encryption.EncryptionType;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.Application;
import software.wings.beans.GitCommit;
import software.wings.beans.KmsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Schema;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapSettings.LdapSettingsKeys;
import software.wings.beans.sso.SSOType;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.exportimport.ExportMode;
import software.wings.dl.exportimport.ImportMode;
import software.wings.dl.exportimport.ImportStatusReport;
import software.wings.dl.exportimport.ImportStatusReport.ImportStatus;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.licensing.LicenseService;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.InstanceStatsCollectorJob;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.scheduler.LimitVicinityCheckerJob;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.AccountPermissionUtils;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class provides REST APIs can be used to export metadata associated with one specific account from one
 * harness cluster and be imported into another harness cluster, as part of effort in migrating account
 * from one cluster to another.
 *
 * @author marklu on 10/24/18
 */
@Api("account")
@Path("/account")
@Scope(ResourceType.SETTING)
@Singleton
@Slf4j
public class AccountExportImportResource {
  private static final String COLLECTION_CONFIG_FILES = "configs.files";
  private static final String COLLECTION_CONFIG_CHUNKS = "configs.chunks";
  private static final String COLLECTION_QUARTZ_JOBS = "quartz_jobs";

  private static final String JSON_FILE_SUFFIX = ".json";
  private static final String ZIP_FILE_SUFFIX = ".zip";

  private WingsMongoPersistence wingsPersistence;
  private Morphia morphia;
  private WingsMongoExportImport mongoExportImport;
  private PersistentScheduler scheduler;
  private AccountService accountService;
  private LicenseService licenseService;
  private AppService appService;
  private UserService userService;
  private AuthService authService;
  private UsageRestrictionsService usageRestrictionsService;
  private AccountPermissionUtils accountPermissionUtils;

  private Map<String, Class<? extends PersistentEntity>> genericExportableEntityTypes = new LinkedHashMap<>();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private JsonParser jsonParser = new JsonParser();

  // Entity classes that need special export handling (e.g. with a special entity filter)
  private static Set<Class<? extends PersistentEntity>> includedEntities = new HashSet<>(
      Arrays.asList(Account.class, User.class, Application.class, Schema.class, EncryptedData.class, KmsConfig.class));

  private static Set<String> includedMongoCollections = new HashSet<>();

  static {
    for (Class<? extends PersistentEntity> entityClazz : includedEntities) {
      includedMongoCollections.add(getCollectionName(entityClazz));
    }
  }

  @Inject
  public AccountExportImportResource(WingsMongoPersistence wingsPersistence, Morphia morphia,
      WingsMongoExportImport mongoExportImport, AccountService accountService, LicenseService licenseService,
      AppService appService, AuthService authService, UsageRestrictionsService usageRestrictionsService,
      UserService userService, AccountPermissionUtils accountPermissionUtils,
      @Named("BackgroundJobScheduler") PersistentScheduler scheduler) {
    this.wingsPersistence = wingsPersistence;
    this.morphia = morphia;
    this.mongoExportImport = mongoExportImport;
    this.scheduler = scheduler;
    this.accountService = accountService;
    this.licenseService = licenseService;
    this.appService = appService;
    this.userService = userService;
    this.authService = authService;
    this.usageRestrictionsService = usageRestrictionsService;
    this.accountPermissionUtils = accountPermissionUtils;

    findExportableEntityTypes();
  }

  @GET
  @Path("/export")
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  public Response exportAccountData(@QueryParam("accountId") final String accountId,
      @QueryParam("mode") @DefaultValue("ALL") ExportMode exportMode,
      @QueryParam("entityTypes") List<String> entityTypes) throws Exception {
    // Only if the user the account administrator or in the Harness user group can perform the export operation.
    if (!userService.isAccountAdmin(accountId)) {
      String errorMessage = "User is not account administrator and can't perform the export operation.";
      RestResponse<Boolean> restResponse = accountPermissionUtils.checkIfHarnessUser(errorMessage);
      if (restResponse != null) {
        logger.error(errorMessage);
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
    }

    if (exportMode == ExportMode.SPECIFIC && isEmpty(entityTypes)) {
      throw new IllegalArgumentException("Export type is ALL but no entity type is specified.");
    }

    String zipFileName = accountId + ZIP_FILE_SUFFIX;
    File zipFile = new File(Files.createTempDir(), zipFileName);
    FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
    ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

    Map<String, Boolean> toBeExported = getToBeExported(exportMode, entityTypes);
    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    // 1. Export harness schema collection.
    String schemaCollectionName = getCollectionName(Schema.class);
    if (isExportable(toBeExported, schemaCollectionName)) {
      DBObject emptyFilter = new BasicDBObject();
      List<String> schemas = mongoExportImport.exportRecords(emptyFilter, schemaCollectionName);
      if (schemas.size() == 0) {
        logger.warn("Schema collection data doesn't exist, schema version data won't be exported.");
      } else {
        exportToStream(zipOutputStream, fileOutputStream, schemas, schemaCollectionName);
      }
    }

    // 2. Export account data.
    String accountCollectionName = getCollectionName(Account.class);
    if (isExportable(toBeExported, accountCollectionName)) {
      DBObject idFilter = new BasicDBObject("_id", accountId);
      List<String> accounts = mongoExportImport.exportRecords(idFilter, accountCollectionName);
      if (accounts.size() == 0) {
        throw new IllegalArgumentException(
            "Account '" + accountId + "' doesn't exist, can't proceed with this export operation.");
      } else {
        exportToStream(zipOutputStream, fileOutputStream, accounts, accountCollectionName);
      }
    }

    // 3. Export all users
    String userCollectionName = getCollectionName(User.class);
    if (isExportable(toBeExported, userCollectionName)) {
      DBObject accountsFilter =
          new BasicDBObject(UserKeys.accounts, new BasicDBObject("$in", new String[] {accountId}));
      List<String> users = mongoExportImport.exportRecords(accountsFilter, userCollectionName);
      exportToStream(zipOutputStream, fileOutputStream, users, userCollectionName);
    }

    DBObject accountIdFilter = new BasicDBObject("accountId", accountId);
    DBObject appIdsFilter = new BasicDBObject("appId", new BasicDBObject("$in", appIds));

    // 4. Export all applications
    String applicationsCollectionName = getCollectionName(Application.class);
    if (isExportable(toBeExported, applicationsCollectionName)) {
      List<String> applications = mongoExportImport.exportRecords(accountIdFilter, applicationsCollectionName);
      exportToStream(zipOutputStream, fileOutputStream, applications, applicationsCollectionName);
    }

    DBObject accountOrAppIdsFilter = new BasicDBObject();
    accountOrAppIdsFilter.put("$or", Arrays.asList(accountIdFilter, appIdsFilter));

    // 5. Export config file content that are persisted in the Mongo GridFs. "configs.files" and "configs.chunks"
    // are not managed by Morphia, need to handle it separately and only export those entries associated with
    // CONFIG_FILE type of secrets for now,
    exportConfigFilesContent(zipOutputStream, fileOutputStream, accountOrAppIdsFilter);

    // 6. Export all other Harness entities that has @Entity annotation excluding what's in the blacklist.
    for (Entry<String, Class<? extends PersistentEntity>> entry : genericExportableEntityTypes.entrySet()) {
      if (isExportable(toBeExported, entry.getKey())) {
        Class<? extends PersistentEntity> entityClazz = entry.getValue();
        if (entityClazz != null) {
          final DBObject exportFilter;
          // 'gitCommits' and 'yamlChangeSet' need special export filter.
          if (GitCommit.class == entityClazz) {
            exportFilter = getGitCommitExportFilter(accountIdFilter);
          } else if (YamlChangeSet.class == entityClazz) {
            exportFilter = getYamlChangeSetExportFilter(accountIdFilter);
          } else {
            exportFilter = accountOrAppIdsFilter;
          }
          String collectionName = getCollectionName(entityClazz);
          List<String> records = mongoExportImport.exportRecords(exportFilter, collectionName);
          exportToStream(zipOutputStream, fileOutputStream, records, collectionName);
        }
      }
    }

    // 7. No need to export Quartz jobs. They can be recreated based on accountId/appId/triggerId etc.
    // Export 'kmsConfig' including the global KMS secret manager if configured (e.g. QA and PROD, but not in fremium
    // yet).
    String kmsConfigCollectionName = getCollectionName(KmsConfig.class);
    if (isExportable(toBeExported, kmsConfigCollectionName)) {
      List<String> accountIdList = Arrays.asList(accountId);
      DBObject exportFilter = new BasicDBObject("accountId", new BasicDBObject("$in", accountIdList));
      List<String> records = mongoExportImport.exportRecords(exportFilter, kmsConfigCollectionName);
      exportToStream(zipOutputStream, fileOutputStream, records, kmsConfigCollectionName);
    }

    logger.info("Flushing exported data into a zip file {}.", zipFileName);
    zipOutputStream.flush();
    zipOutputStream.close();
    fileOutputStream.flush();
    fileOutputStream.close();
    logger.info(
        "Finished flushing {} bytes of exported account data into a zip file {}.", zipFile.length(), zipFileName);

    return Response.ok(zipFile, MediaType.APPLICATION_OCTET_STREAM)
        .header("content-disposition", "attachment; filename = " + zipFileName)
        .build();
  }

  private boolean isExportable(Map<String, Boolean> toBeExported, String collectionName) {
    Boolean exportable = toBeExported.get(collectionName);
    return exportable != null && exportable;
  }

  private void exportToStream(ZipOutputStream zipOutputStream, FileOutputStream fileOutputStream, List<String> records,
      String collectionName) throws IOException {
    String zipEntryName = collectionName + JSON_FILE_SUFFIX;
    ZipEntry zipEntry = new ZipEntry(zipEntryName);
    logger.info("Zipping entry: {}", zipEntryName);
    zipOutputStream.putNextEntry(zipEntry);
    JsonArray jsonArray = convertStringListToJsonArray(records);
    String jsonString = gson.toJson(jsonArray);
    zipOutputStream.write(jsonString.getBytes(Charset.defaultCharset()));
    // Flush when each collection finished exporting into the stream to reduce memory footprint.
    zipOutputStream.flush();
    fileOutputStream.flush();
    logger.info("{} '{}' records have been exported.", records.size(), collectionName);
  }

  private DBObject getGitCommitExportFilter(DBObject accountIdFilter) {
    // 'gitCommits' within last 1 month should be exported.
    DBObject createdAtFilter = new BasicDBObject(
        "createdAt", new BasicDBObject("$gt", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)));
    return new BasicDBObject("$and", Arrays.asList(accountIdFilter, createdAtFilter));
  }

  private DBObject getYamlChangeSetExportFilter(DBObject accountIdFilter) {
    // 'yamlChangeSet' in QUEUED status should be exported.
    DBObject statusQueuedFilter = new BasicDBObject("status", Status.QUEUED.name());
    return new BasicDBObject("$and", Arrays.asList(accountIdFilter, statusQueuedFilter));
  }

  private void exportConfigFilesContent(ZipOutputStream zipOutputStream, FileOutputStream fileOutputStream,
      DBObject accountOrAppIdsFilter) throws IOException {
    // 1. Export EncryptedData records
    String encryptedDataCollectionName = getCollectionName(EncryptedData.class);
    List<String> encryptedDataRecords =
        mongoExportImport.exportRecords(accountOrAppIdsFilter, encryptedDataCollectionName);
    exportToStream(zipOutputStream, fileOutputStream, encryptedDataRecords, encryptedDataCollectionName);

    // 2. Find out all file IDs referred by KMS/CONFIG_FILE encrypted records.
    List<ObjectId> configFileIds = new ArrayList<>();
    if (isNotEmpty(encryptedDataRecords)) {
      for (String encryptedDataRecord : encryptedDataRecords) {
        JsonElement encryptedDataElement = jsonParser.parse(encryptedDataRecord);
        // Only KMS type of encrypted records have file content saved in File serivce/GridFS, which need to be exported.
        EncryptionType encryptionType =
            EncryptionType.valueOf(encryptedDataElement.getAsJsonObject().get("encryptionType").getAsString());
        SettingVariableTypes settingVariableType =
            SettingVariableTypes.valueOf(encryptedDataElement.getAsJsonObject().get("type").getAsString());
        if (encryptionType == EncryptionType.KMS && settingVariableType == SettingVariableTypes.CONFIG_FILE) {
          String fileId = encryptedDataElement.getAsJsonObject().get("encryptedValue").getAsString();
          ObjectId objectId = getObjectIdFromFileId(fileId);
          if (objectId != null) {
            configFileIds.add(new ObjectId(fileId));
          }
        }
      }
    }

    // 3. Export all 'configs.files' records in the configFileIds list.
    DBObject inIdsFilter = new BasicDBObject("_id", new BasicDBObject("$in", configFileIds));
    List<String> configFilesRecords = mongoExportImport.exportRecords(inIdsFilter, COLLECTION_CONFIG_FILES);
    exportToStream(zipOutputStream, fileOutputStream, configFilesRecords, COLLECTION_CONFIG_FILES);

    // 4. Export all 'configs.files' records in the configFileIds list.
    DBObject inFilesIdFilter = new BasicDBObject("files_id", new BasicDBObject("$in", configFileIds));
    List<String> configChunkRecords = mongoExportImport.exportRecords(inFilesIdFilter, COLLECTION_CONFIG_CHUNKS);
    exportToStream(zipOutputStream, fileOutputStream, configChunkRecords, COLLECTION_CONFIG_CHUNKS);
  }

  @POST
  @Path("/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  public RestResponse<ImportStatusReport> importAccountData(@QueryParam("accountId") final String accountId,
      @QueryParam("mode") @DefaultValue("UPSERT") ImportMode importMode,
      @QueryParam("disableSchemaCheck") boolean disableSchemaCheck, @QueryParam("adminUser") String adminUserEmail,
      @QueryParam("adminPassword") String adminPassword, @QueryParam("accountName") String newAccountName,
      @QueryParam("companyName") String newCompanyName, @FormDataParam("file") final InputStream uploadInputStream)
      throws Exception {
    // Only if the user the account administrator or in the Harness user group can perform the export operation.
    if (!userService.isAccountAdmin(accountId)) {
      String errorMessage = "User is not account administrator and can't perform the import operation.";
      RestResponse<ImportStatusReport> restResponse = accountPermissionUtils.checkIfHarnessUser(errorMessage);
      if (restResponse != null) {
        logger.error(errorMessage);
        return restResponse;
      }
    }

    logger.info("Started importing data for account '{}'.", accountId);
    Map<String, String> zipEntryDataMap = readZipEntries(uploadInputStream);
    logger.info("Finished reading uploaded input stream in zip format.");

    List<ImportStatus> importStatuses = new ArrayList<>();

    // 1. Check schema version compatibility. Would enforce the exported collections' schema is the same as the current
    // installation's
    if (!disableSchemaCheck) {
      checkSchemaVersionCompatibility(zipEntryDataMap);
    }

    // 2.1 Preserve the account license expiration, license unit etc. information.
    Account account = accountService.get(accountId);
    // LicenseInfo should not be null for migrated Freemium accounts
    LicenseInfo licenseInfo = account.getLicenseInfo();

    // 2.2 Import account data first.
    String accountCollectionName = getCollectionName(Account.class);
    JsonArray accounts = getJsonArray(zipEntryDataMap, accountCollectionName);
    if (accounts != null) {
      importStatuses.add(
          mongoExportImport.importRecords(accountCollectionName, convertJsonArrayToStringList(accounts), importMode));
    }

    // 3. Import users
    String userCollectionName = getCollectionName(User.class);
    JsonArray users = getJsonArray(zipEntryDataMap, userCollectionName);
    // Find potential user email clashes and find the mapping of imported user id to existing user id.
    Map<String, String> clashedUserIdMapping = findClashedUserIdMapping(accountId, users);
    if (users != null) {
      importStatuses.add(
          mongoExportImport.importRecords(userCollectionName, convertJsonArrayToStringList(users), importMode));
    }

    // 4. Import applications
    String applicationsCollectionName = getCollectionName(Application.class);
    JsonArray applications = getJsonArray(zipEntryDataMap, applicationsCollectionName, clashedUserIdMapping);
    if (applications != null) {
      importStatuses.add(mongoExportImport.importRecords(
          applicationsCollectionName, convertJsonArrayToStringList(applications), importMode));
    }

    // 5. Import all "encryptedRecords", "configs.file" and "configs.chunks" content
    String encryptedDataCollectionName = getCollectionName(EncryptedData.class);
    JsonArray encryptedData = getJsonArray(zipEntryDataMap, encryptedDataCollectionName, clashedUserIdMapping);
    if (encryptedData != null) {
      importStatuses.add(mongoExportImport.importRecords(
          encryptedDataCollectionName, convertJsonArrayToStringList(encryptedData), importMode));
    }
    JsonArray configFiles = getJsonArray(zipEntryDataMap, COLLECTION_CONFIG_FILES);
    if (configFiles != null) {
      ImportStatus importStatus = mongoExportImport.importRecords(
          COLLECTION_CONFIG_FILES, convertJsonArrayToStringList(configFiles), importMode);
      if (importStatus != null) {
        importStatuses.add(importStatus);
      }
    }
    JsonArray configChunks = getJsonArray(zipEntryDataMap, COLLECTION_CONFIG_CHUNKS);
    if (configChunks != null) {
      ImportStatus importStatus = mongoExportImport.importRecords(
          COLLECTION_CONFIG_CHUNKS, convertJsonArrayToStringList(configChunks), importMode);
      if (importStatus != null) {
        importStatuses.add(importStatus);
      }
    }

    // 6. Import all other entity types.
    for (Entry<String, Class<? extends PersistentEntity>> entry : genericExportableEntityTypes.entrySet()) {
      String collectionName = getCollectionName(entry.getValue());
      JsonArray jsonArray = getJsonArray(zipEntryDataMap, collectionName, clashedUserIdMapping);
      if (jsonArray == null) {
        logger.info("No data found for collection '{}' to import.", collectionName);
      } else {
        ImportStatus importStatus =
            mongoExportImport.importRecords(collectionName, convertJsonArrayToStringList(jsonArray), importMode);
        if (importStatus != null) {
          importStatuses.add(importStatus);
        }
      }
    }

    // 7. import kmsConfig as a special handling.
    String kmsConfigCollectionName = getCollectionName(KmsConfig.class);
    JsonArray kmsConfigs = getJsonArray(zipEntryDataMap, kmsConfigCollectionName, clashedUserIdMapping);
    if (kmsConfigs != null) {
      ImportStatus importStatus = mongoExportImport.importRecords(
          kmsConfigCollectionName, convertJsonArrayToStringList(kmsConfigs), importMode);
      if (importStatus != null) {
        importStatuses.add(importStatus);
      }
    }

    // 8. Update license to the previously set license (unit, type etc.)
    if (licenseInfo != null) {
      licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
      licenseService.updateAccountLicense(accountId, licenseInfo);
      logger.info("Updated license of account {} to: {}", accountId, licenseInfo);
    }

    // 9. Update the first account administrator's password for post-migration validation by CSE team if specified
    if (!StringUtils.isEmpty(adminUserEmail) && !StringUtils.isEmpty(adminPassword)) {
      updateAdminUserPassword(accountId, adminUserEmail, adminPassword);
    }

    // PL-3126: When the 'accountName' query parameter is provided, it means the account name need to be renamed at
    // account migration/import time.
    if (isNotEmpty(newAccountName)) {
      accountService.updateAccountName(accountId, newAccountName, newCompanyName);
    }

    // 10. Reinstantiate Quartz jobs (recreate through APIs) in the new cluster
    reinstantiateQuartzJobs(accountId, importStatuses);

    logger.info("{} collections has been imported.", importStatuses.size());
    logger.info("Finished importing data for account '{}'.", accountId);

    return new RestResponse<>(ImportStatusReport.builder().statuses(importStatuses).mode(importMode).build());
  }

  private void updateAdminUserPassword(String accountId, String adminUserEmail, String adminPassword) {
    User adminUser = userService.getUserByEmail(adminUserEmail);
    if (adminUser != null) {
      String adminUserId = adminUser.getUuid();
      // CSE should notify the admin user of the customer to reset password after validated the migrated account.
      String hashed = hashpw(adminPassword, BCrypt.gensalt());
      wingsPersistence.update(adminUser,
          wingsPersistence.createUpdateOperations(User.class)
              .set(UserKeys.passwordHash, hashed)
              .set(UserKeys.passwordExpired, false)
              .set(UserKeys.passwordChangedAt, System.currentTimeMillis()));
      authService.invalidateAllTokensForUser(adminUserId);

      logger.info("Updated password of admin user {} with id {} for account {} during account import",
          adminUser.getEmail(), adminUserId, accountId);
    }
  }

  private void reinstantiateQuartzJobs(String accountId, List<ImportStatus> importStatuses) {
    // 1. Recreate account level jobs
    AlertCheckJob.addWithDelay(scheduler, accountId);
    InstanceStatsCollectorJob.addWithDelay(scheduler, accountId);
    LimitVicinityCheckerJob.addWithDelay(scheduler, accountId);

    // Recreate application or lower level jobs each need some special handling.
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    int importedJobCount = 0;

    // 3. ScheduledTriggerJob
    List<Trigger> triggers = getAllScheduledTriggersForAccount(accountId, appIds);
    for (Trigger trigger : triggers) {
      // Scheduled triggers is using the cron expression as trigger. No need to add special delay.
      ScheduledTriggerJob.add(scheduler, accountId, trigger.getAppId(), trigger.getUuid(), trigger);
      importedJobCount++;
    }

    // 4. LdapGroupSyncJob
    List<LdapSettings> ldapSettings = getAllLdapSettingsForAccount(accountId);
    for (LdapSettings ldapSetting : ldapSettings) {
      LdapGroupSyncJob.add(scheduler, accountId, ldapSetting.getUuid());
      importedJobCount++;
    }

    logger.info("{} cron jobs has been recreated.", importedJobCount);

    if (importedJobCount > 0) {
      ImportStatus importStatus =
          ImportStatus.builder().collectionName(COLLECTION_QUARTZ_JOBS).imported(importedJobCount).build();
      importStatuses.add(importStatus);
    }
  }

  Map<String, String> findClashedUserIdMapping(String accountId, JsonArray users) {
    // The users to be imported might have the same email with existing user in the cluster it's being imported into.
    // This method will build the mapping between these clashed user ids. Upon occurrence of user clash with the same
    // email:
    // 1. The user with email clash won't be imported.
    // 2. The existing user with the same email need to be added to the account to be exported.
    Map<String, String> userIdMapping = new HashMap<>();
    if (users != null && users.size() > 0) {
      Account account = wingsPersistence.get(Account.class, accountId);
      for (JsonElement user : users) {
        JsonObject userObject = user.getAsJsonObject();
        String userId = userObject.get("_id").getAsString();
        final String email = userObject.get("email").getAsString();
        if (isEmpty(email)) {
          String userName = userObject.get("name").getAsString();
          // Ignore as this user doesn't have an email attribute
          logger.info("User '{}' with id {} doesn't have an email attribute, it will be skipped from being imported.",
              userName, userId);
          continue;
        }
        User existingUser = userService.getUserByEmail(email);
        if (existingUser != null && !existingUser.getUuid().equals(userId)) {
          userIdMapping.put(userId, existingUser.getUuid());
          logger.info(
              "User '{}' with email '{}' clashes with one existing user '{}'.", userId, email, existingUser.getUuid());
          // Adding the new import account into the account list of the existing user.
          existingUser.getAccounts().add(account);
          wingsPersistence.save(existingUser);
        }
      }
      if (userIdMapping.size() > 0) {
        logger.info(
            "{} users have email clashes with existing users and all of the references to it in the imported records need to be replaced.",
            userIdMapping.size());
      }
    }
    return userIdMapping;
  }

  String replaceClashedUserIds(String collectionJson, Map<String, String> clashedUserIdMapping) {
    // 3. All references to the old user id in all records to be imported need to be replaced with the new user id
    // Typical reference to user ids are in 'createdBy/uuid', 'lastUpdatedBy/uuid' and user group membership.
    String result = collectionJson;
    if (isNotEmpty(clashedUserIdMapping)) {
      for (Entry<String, String> idMappingEntry : clashedUserIdMapping.entrySet()) {
        result = result.replaceAll("\"" + idMappingEntry.getKey() + "\"", "\"" + idMappingEntry.getValue() + "\"");
      }
    }
    return result;
  }

  private List<Trigger> getAllScheduledTriggersForAccount(String accountId, List<String> appIds) {
    List<Trigger> triggers = new ArrayList<>();
    Query<Trigger> query = wingsPersistence.createQuery(Trigger.class).filter("appId in", appIds);
    try (HIterator<Trigger> iterator = new HIterator<>(query.fetch())) {
      for (Trigger trigger : iterator) {
        if (trigger.getCondition().getConditionType() == TriggerConditionType.SCHEDULED) {
          triggers.add(trigger);
        }
      }
    }
    return triggers;
  }

  private List<LdapSettings> getAllLdapSettingsForAccount(String accountId) {
    List<LdapSettings> ldapSettings = new ArrayList<>();
    Query<LdapSettings> query = wingsPersistence.createQuery(LdapSettings.class)
                                    .filter(LdapSettingsKeys.accountId, accountId)
                                    .filter("type", SSOType.LDAP);
    Iterator<LdapSettings> iterator = query.iterator();
    while (iterator.hasNext()) {
      ldapSettings.add(iterator.next());
    }

    return ldapSettings;
  }

  private Map<String, String> readZipEntries(InputStream inputStream) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
      Map<String, String> collectionDataMap = new HashMap<>();
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int len = 0;
        byte[] buffer = new byte[1024];
        while ((len = zipInputStream.read(buffer)) > 0) {
          outputStream.write(buffer, 0, len);
        }
        collectionDataMap.put(zipEntry.getName(), new String(outputStream.toByteArray(), Charset.defaultCharset()));
      }

      return collectionDataMap;
    }
  }

  private JsonArray getJsonArray(Map<String, String> zipDataMap, String collectionName) {
    return getJsonArray(zipDataMap, collectionName, null);
  }

  private JsonArray getJsonArray(
      Map<String, String> zipDataMap, String collectionName, Map<String, String> clashedUserIdMapping) {
    String zipEntryName = collectionName + JSON_FILE_SUFFIX;
    String json = zipDataMap.get(zipEntryName);
    if (isNotEmpty(json)) {
      // Replace clashed user id with the new user id in the current system.
      json = replaceClashedUserIds(json, clashedUserIdMapping);
      return (JsonArray) jsonParser.parse(json);
    } else {
      return null;
    }
  }

  private List<String> convertJsonArrayToStringList(JsonArray jsonArray) {
    List<String> result = new ArrayList<>(jsonArray.size());
    for (JsonElement jsonElement : jsonArray) {
      result.add(jsonElement.toString());
    }
    return result;
  }

  private JsonArray convertStringListToJsonArray(List<String> records) {
    JsonArray jsonArray = new JsonArray();
    for (String record : records) {
      jsonArray.add(jsonParser.parse(record));
    }
    return jsonArray;
  }

  private ObjectId getObjectIdFromFileId(String fileId) {
    try {
      return new ObjectId(fileId);
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid BSON object id: " + fileId);
      return null;
    }
  }

  private JsonObject getSchema(JsonParser jsonParser) {
    DBObject idFilter = new BasicDBObject("_id", "schema");
    List<String> schemas = mongoExportImport.exportRecords(idFilter, getCollectionName(Schema.class));
    if (schemas.size() == 0) {
      throw new IllegalArgumentException("Schema collection doesn't exist, can't proceed with this export operation.");
    } else {
      String schemaJson = schemas.get(0);
      return jsonParser.parse(schemaJson).getAsJsonObject();
    }
  }

  private int getSchemaVersion(JsonObject schema) {
    return schema.get("version").getAsInt();
  }

  private void checkSchemaVersionCompatibility(Map<String, String> zipDataMap) {
    String collectionName = getCollectionName(Schema.class);
    JsonArray schemas = getJsonArray(zipDataMap, collectionName);
    if (schemas != null) {
      // Schema version. There should be only one entry.
      int importSchemaVersion = getSchemaVersion(schemas.get(0).getAsJsonObject());
      int currentSchemaVersion = getSchemaVersion(getSchema(jsonParser));
      logger.info("Import schema version is {}; Current cluster's schema version is {}.", importSchemaVersion,
          currentSchemaVersion);
      if (importSchemaVersion == currentSchemaVersion) {
        logger.info("Schema compatibility check has passed. Proceed further to import account data.");
      } else {
        throw new WingsException("Incompatible schema version! Import schema version: " + importSchemaVersion
            + "; Current schema version: " + currentSchemaVersion);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void findExportableEntityTypes() {
    morphia.getMapper().getMappedClasses().forEach(mc -> {
      Class<? extends PersistentEntity> clazz = (Class<? extends PersistentEntity>) mc.getClazz();
      if (mc.getEntityAnnotation() != null && isAnnotatedExportable(clazz)) {
        // Find out non-abstract classes with both 'Entity' and 'HarnessEntity' annotation.
        String mongoCollectionName = mc.getEntityAnnotation().value();
        if (!includedMongoCollections.contains(mongoCollectionName)) {
          logger.debug("Collection '{}' is exportable", mongoCollectionName);
          genericExportableEntityTypes.put(mongoCollectionName, clazz);
        }
      }
    });
  }

  private boolean isAnnotatedExportable(Class<? extends PersistentEntity> clazz) {
    HarnessEntity harnessEntity = clazz.getAnnotation(HarnessEntity.class);
    return harnessEntity != null && harnessEntity.exportable();
  }

  private Map<String, Boolean> getToBeExported(ExportMode exportType, List<String> entityTypes) {
    Map<String, Boolean> toBeExported = new HashMap<>();
    switch (exportType) {
      case ALL:
        for (String exportableEntityType : genericExportableEntityTypes.keySet()) {
          toBeExported.put(exportableEntityType, Boolean.TRUE);
        }
        for (String includedEntityType : includedMongoCollections) {
          toBeExported.put(includedEntityType, Boolean.TRUE);
        }
        break;
      case SPECIFIC:
        for (String exportableEntityType : genericExportableEntityTypes.keySet()) {
          toBeExported.put(exportableEntityType, Boolean.FALSE);
        }
        for (String includedEntityType : includedMongoCollections) {
          toBeExported.put(includedEntityType, Boolean.FALSE);
        }
        for (String entityType : entityTypes) {
          toBeExported.put(entityType, Boolean.TRUE);
        }
        break;
      default:
        throw new IllegalArgumentException("Export type " + exportType + " is not supported.");
    }
    return toBeExported;
  }
}
