package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.harness.exception.WingsException;
import io.harness.persistence.PersistentEntity;
import io.harness.queue.Queuable;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.WaitInstance;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import software.wings.audit.AuditHeader;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask;
import software.wings.beans.EntityVersionCollection;
import software.wings.beans.FeatureFlag;
import software.wings.beans.Idempotent;
import software.wings.beans.Log;
import software.wings.beans.Notification;
import software.wings.beans.RestResponse;
import software.wings.beans.Schema;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SweepingOutput;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.Alert;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.template.TemplateVersion;
import software.wings.dl.exportimport.ExportMode;
import software.wings.dl.exportimport.ImportMode;
import software.wings.dl.exportimport.ImportStatusReport;
import software.wings.dl.exportimport.ImportStatusReport.ImportStatus;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.security.EncryptionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.AppService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.yaml.errorhandling.GitSyncError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
  private static final String JSON_FILE_SUFFIX = ".json";
  private static final String ZIP_FILE_SUFFIX = ".zip";

  private WingsMongoExportImport mongoExportImport;
  private AppService appService;
  private Map<String, Class<? extends Base>> genericExportableEntityTypes = new LinkedHashMap<>();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private JsonParser jsonParser = new JsonParser();

  private static Set<Class<? extends PersistentEntity>> includedEntities =
      new HashSet<>(Arrays.asList(Account.class, User.class, Application.class, Schema.class, EncryptedData.class));

  private static Set<Class<? extends PersistentEntity>> excludedEntities = new HashSet<>(Arrays.asList(
      // Execution/Command/Audit logs
      Log.class, LogDataRecord.class, ThirdPartyApiCallLog.class, AuditHeader.class,
      // Notification/alert records
      Notification.class, Alert.class, NotifyResponse.class,
      // Learning engine related runtime data records
      LogMLAnalysisRecord.class, LogMLFeedbackRecord.class, ExperimentalLogMLAnalysisRecord.class,
      EntityVersionCollection.class, TimeSeriesMLAnalysisRecord.class, LearningEngineAnalysisTask.class,
      LearningEngineExperimentalAnalysisTask.class, MLExperiments.class, NewRelicMetricDataRecord.class,
      ExperimentalMetricAnalysisRecord.class, ContinuousVerificationExecutionMetaData.class,
      // Locks and Tasks
      Idempotent.class, WaitInstance.class, DelegateTask.class,
      // Global entities not associated with any account
      FeatureFlag.class, TemplateVersion.class,
      // Runtime data/status etc.
      StateExecutionInstance.class, ServiceInstance.class, WorkflowExecution.class, InstanceStatsSnapshot.class,
      StateMachine.class, SweepingOutput.class, ContainerTask.class, GitSyncError.class, ManualSyncJob.class,
      SyncStatus.class

      ));

  private static Set<String> includedMongoCollections = new HashSet<>();
  private static Set<String> excludedMongoCollections = new HashSet<>();

  static {
    for (Class<? extends PersistentEntity> entityClazz : includedEntities) {
      includedMongoCollections.add(getCollectionName(entityClazz));
    }
    for (Class<? extends PersistentEntity> entityClazz : excludedEntities) {
      excludedMongoCollections.add(getCollectionName(entityClazz));
    }
  }

  @Inject
  public AccountExportImportResource(WingsMongoExportImport mongoExportImport, AppService appService) {
    this.mongoExportImport = mongoExportImport;
    this.appService = appService;

    findExportableEntityTypes();
  }

  @GET
  @Path("/export")
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  public Response exportAccountData(@QueryParam("accountId") final String accountId,
      @QueryParam("mode") @DefaultValue("ALL") ExportMode exportMode,
      @QueryParam("entityTypes") List<String> entityTypes) throws Exception {
    if (exportMode == ExportMode.SPECIFIC && isEmpty(entityTypes)) {
      throw new IllegalArgumentException("Export type is ALL but no entity type is specified.");
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

    Map<String, Boolean> toBeExported = getToBeExported(exportMode, entityTypes);
    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    // 1. Export harness schema collection.
    String schemaCollectionName = getCollectionName(Schema.class);
    if (isExportable(toBeExported, schemaCollectionName)) {
      DBObject emptyFilter = new BasicDBObject();
      List<String> schemas = mongoExportImport.exportRecords(emptyFilter, schemaCollectionName);
      if (schemas.size() == 0) {
        log.warn("Schema collection data doesn't exist, schema version data won't be exported.");
      } else {
        exportToStream(zipOutputStream, schemas, schemaCollectionName);
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
        exportToStream(zipOutputStream, accounts, accountCollectionName);
      }
    }

    // 3. Export all users
    String userCollectionName = getCollectionName(User.class);
    if (isExportable(toBeExported, userCollectionName)) {
      DBObject accountsFilter = new BasicDBObject("accounts", new BasicDBObject("$in", new String[] {accountId}));
      List<String> users = mongoExportImport.exportRecords(accountsFilter, userCollectionName);
      exportToStream(zipOutputStream, users, userCollectionName);
    }

    DBObject accountIdFilter = new BasicDBObject("accountId", accountId);
    DBObject appIdsFilter = new BasicDBObject("appId", new BasicDBObject("$in", appIds));

    // 4. Export all applications
    String applicationsCollectionName = getCollectionName(Application.class);
    if (isExportable(toBeExported, applicationsCollectionName)) {
      List<String> applications = mongoExportImport.exportRecords(accountIdFilter, applicationsCollectionName);
      exportToStream(zipOutputStream, applications, applicationsCollectionName);
    }

    List<DBObject> orFilterList = new ArrayList<>();
    orFilterList.add(accountIdFilter);
    orFilterList.add(appIdsFilter);

    DBObject accountOrAppIdsFilter = new BasicDBObject();
    accountOrAppIdsFilter.put("$or", orFilterList);

    // 5. Export config file content that are persisted in the Mongo GridFs. "configs.files" and "configs.chunks"
    // are not managed by Morphia, need to handle it separately and only export those entries associated with
    // CONFIG_FILE type of secrets for now,
    exportConfigFilesContent(zipOutputStream, accountOrAppIdsFilter);

    // 6. Export all other Harness entities that has @Entity annotation excluding what's in the blacklist.
    for (Entry<String, Class<? extends Base>> entry : genericExportableEntityTypes.entrySet()) {
      if (isExportable(toBeExported, entry.getKey())) {
        Class<? extends Base> entityClazz = entry.getValue();
        if (entityClazz != null) {
          String collectionName = getCollectionName(entityClazz);
          List<String> records = mongoExportImport.exportRecords(accountOrAppIdsFilter, collectionName);
          exportToStream(zipOutputStream, records, collectionName);
        }
      }
    }

    String zipFileName = accountId + ZIP_FILE_SUFFIX;
    zipOutputStream.flush();
    zipOutputStream.close();

    return Response.ok(outputStream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM)
        .header("content-disposition", "attachment; filename = " + zipFileName)
        .build();
  }

  private boolean isExportable(Map<String, Boolean> toBeExported, String collectionName) {
    Boolean exportable = toBeExported.get(collectionName);
    return exportable != null && exportable;
  }

  private void exportToStream(ZipOutputStream zipOutputStream, List<String> records, String collectionName)
      throws IOException {
    String zipEntryName = collectionName + JSON_FILE_SUFFIX;
    ZipEntry zipEntry = new ZipEntry(zipEntryName);
    log.info("Zipping entry: {}", zipEntryName);
    zipOutputStream.putNextEntry(zipEntry);
    JsonArray jsonArray = convertStringListToJsonArray(records);
    String jsonString = gson.toJson(jsonArray);
    zipOutputStream.write(jsonString.getBytes(Charset.defaultCharset()));
  }

  private void exportConfigFilesContent(ZipOutputStream zipOutputStream, DBObject accountOrAppIdsFilter)
      throws IOException {
    // 1. Export EncryptedData records
    String encryptedDataCollectionName = getCollectionName(EncryptedData.class);
    List<String> encryptedDataRecords =
        mongoExportImport.exportRecords(accountOrAppIdsFilter, encryptedDataCollectionName);
    exportToStream(zipOutputStream, encryptedDataRecords, encryptedDataCollectionName);

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
    exportToStream(zipOutputStream, configFilesRecords, COLLECTION_CONFIG_FILES);

    // 4. Export all 'configs.files' records in the configFileIds list.
    DBObject inFilesIdFilter = new BasicDBObject("files_id", new BasicDBObject("$in", configFileIds));
    List<String> configChunkRecords = mongoExportImport.exportRecords(inFilesIdFilter, COLLECTION_CONFIG_CHUNKS);
    exportToStream(zipOutputStream, configChunkRecords, COLLECTION_CONFIG_CHUNKS);
  }

  @POST
  @Path("/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  public RestResponse<ImportStatusReport> importAccountData(@QueryParam("accountId") final String accountId,
      @QueryParam("mode") @DefaultValue("UPSERT") ImportMode importMode,
      @FormDataParam("file") final InputStream uploadInputStream) throws Exception {
    Map<String, String> zipEntryDataMap = readZipEntries(uploadInputStream);

    List<ImportStatus> importStatuses = new ArrayList<>();

    // 1. Check schema version compatibility. Would enforce the exported collections' schema is the same as the current
    // installation's
    checkSchemaVersionCompatibility(zipEntryDataMap);

    // 2. Import account data first.
    String accountCollectionName = getCollectionName(Account.class);
    JsonArray accounts = getJsonArray(zipEntryDataMap, accountCollectionName);
    if (accounts != null) {
      importStatuses.add(mongoExportImport.importRecords(
          accountCollectionName, convertJsonArrayToStringList(accounts), importMode, new String[] {"accountName"}));
    }

    // 3. Import users
    String userCollectionName = getCollectionName(User.class);
    JsonArray users = getJsonArray(zipEntryDataMap, userCollectionName);
    if (users != null) {
      importStatuses.add(
          mongoExportImport.importRecords(userCollectionName, convertJsonArrayToStringList(users), importMode));
    }

    // 4. Import applications
    String applicationsCollectionName = getCollectionName(Application.class);
    JsonArray applications = getJsonArray(zipEntryDataMap, applicationsCollectionName);
    if (applications != null) {
      importStatuses.add(mongoExportImport.importRecords(
          applicationsCollectionName, convertJsonArrayToStringList(applications), importMode));
    }

    // 5. Import all "encryptedRecords", "configs.file" and "configs.chunks" content
    String encryptedDataCollectionName = getCollectionName(EncryptedData.class);
    JsonArray encryptedData = getJsonArray(zipEntryDataMap, encryptedDataCollectionName);
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
    for (Entry<String, Class<? extends Base>> entry : genericExportableEntityTypes.entrySet()) {
      String collectionName = getCollectionName(entry.getValue());
      JsonArray jsonArray = getJsonArray(zipEntryDataMap, collectionName);
      ImportStatus importStatus =
          mongoExportImport.importRecords(collectionName, convertJsonArrayToStringList(jsonArray), importMode);
      if (importStatus != null) {
        importStatuses.add(importStatus);
      }
    }

    return new RestResponse<>(ImportStatusReport.builder().statuses(importStatuses).mode(importMode).build());
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
    String zipEntryName = collectionName + JSON_FILE_SUFFIX;
    String json = zipDataMap.get(zipEntryName);
    if (isNotEmpty(json)) {
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
      log.warn("Invalid BSON object id: " + fileId);
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
      log.info("Import schema version is {}; Current cluster's schema version is {}.", importSchemaVersion,
          currentSchemaVersion);
      if (importSchemaVersion == currentSchemaVersion) {
        log.info("Schema compatibility check has passed. Proceed further to import account data.");
      } else {
        throw new WingsException("Incompatible schema version! Import schema version: " + importSchemaVersion
            + "; Current schema version: " + currentSchemaVersion);
      }
    }
  }

  private static String getCollectionName(Class<? extends PersistentEntity> clazz) {
    return clazz.getAnnotation(Entity.class).value();
  }

  @SuppressWarnings("unchecked")
  private void findExportableEntityTypes() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");

    morphia.getMapper().getMappedClasses().forEach(mc -> {
      if (mc.getEntityAnnotation() != null && !mc.isAbstract()) {
        // Read class level "Entity" annotation
        String mongoCollectionName = mc.getEntityAnnotation().value();
        if (!excludedMongoCollections.contains(mongoCollectionName)
            && !includedMongoCollections.contains(mongoCollectionName)) {
          Class<? extends Base> clazz = (Class<? extends Base>) mc.getClazz();
          // Queuable entity types don't need to be exported.
          if (clazz != null && !Queuable.class.isAssignableFrom(clazz) && Base.class.isAssignableFrom(clazz)) {
            log.debug("Collection '{}' is exportable", mongoCollectionName);
            genericExportableEntityTypes.put(mongoCollectionName, clazz);
          }
        }
      }
    });
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
