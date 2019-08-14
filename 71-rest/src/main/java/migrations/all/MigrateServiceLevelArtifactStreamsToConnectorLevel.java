package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.CustomArtifactServerConfig;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.dl.WingsPersistence;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.UsageRestrictions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@SuppressWarnings("deprecation")
public class MigrateServiceLevelArtifactStreamsToConnectorLevel implements Migration {
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w"; // TODO: change this to reflect correct account
  private static final String SEPARATOR = "_";
  private static final String CUSTOM_ARTIFACT_SERVER = "Custom Artifact Server";
  private static int COUNTER = 1;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public void migrate() {
    logger.info("Migration Started - move service level artifact streams to connector level for account " + ACCOUNT_ID);
    Account account = wingsPersistence.get(Account.class, ACCOUNT_ID);
    if (account == null) {
      logger.info("Specified account not found. Not migrating artifact streams from services to connectors.");
      return;
    }
    // Prefetch applications for this account
    List<Application> applications = wingsPersistence.createQuery(Application.class)
                                         .filter(ApplicationKeys.accountId, ACCOUNT_ID)
                                         .project(ApplicationKeys.name, true)
                                         .asList();
    Map<String, String> appIdToNameMap = new HashMap<>();
    for (Application application : applications) {
      appIdToNameMap.put(application.getUuid(), application.getName());
    }
    // Get all services for account
    Map<String, String> serviceIdToNameMap = new HashMap<>();
    List<Service> services = wingsPersistence.createQuery(Service.class)
                                 .field(ServiceKeys.appId)
                                 .in(appIdToNameMap.keySet())
                                 .project(ServiceKeys.name, true)
                                 .asList();
    for (Service service : services) {
      serviceIdToNameMap.put(service.getUuid(), service.getName());
    }

    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "artifactStream");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int updated = 0;
    boolean found = false;
    try (HIterator<SettingAttribute> settingAttributeHIterator =
             new HIterator<>(wingsPersistence.createQuery(SettingAttribute.class)
                                 .filter(SettingAttributeKeys.accountId, ACCOUNT_ID)
                                 .fetch())) {
      while (settingAttributeHIterator.hasNext()) {
        SettingAttribute settingAttribute = settingAttributeHIterator.next();

        // For each SettingId find all the artifact Steams
        logger.info("Finding all artifact streams associated with settingId {}", settingAttribute.getUuid());
        try (HIterator<ArtifactStream> artifactStreamsIterator =
                 new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                     .filter(ArtifactStreamKeys.settingId, settingAttribute.getUuid())
                                     .field(ArtifactStreamKeys.appId)
                                     .notEqual(GLOBAL_APP_ID)
                                     .fetch())) {
          while (artifactStreamsIterator.hasNext()) {
            found = true;
            ArtifactStream artifactStream = artifactStreamsIterator.next();
            // Resolve the duplicate names
            // Name : settingname_appname_service_name_artifactStreamName_type
            // Make APP_ID as GLOBAL_APP_ID
            // serviceId can be settingId
            // bulk update
            String name = settingAttribute.getName() + SEPARATOR + appIdToNameMap.get(artifactStream.getAppId())
                + SEPARATOR + serviceIdToNameMap.get(artifactStream.getServiceId()) + SEPARATOR + "AS" + COUNTER++
                + SEPARATOR + artifactStream.getArtifactStreamType();
            bulkWriteOperation
                .find(wingsPersistence.createQuery(ArtifactStream.class)
                          .filter(ArtifactStreamKeys.uuid, artifactStream.getUuid())
                          .getQueryObject())
                .updateOne(new BasicDBObject("$set",
                    new BasicDBObject(ArtifactStreamKeys.name, name)
                        .append(ArtifactStreamKeys.appId, GLOBAL_APP_ID)
                        .append(ArtifactStreamKeys.serviceId, settingAttribute.getUuid())));
            updated++;
          }
        }
        if (found) {
          bulkWriteOperation.execute();
          logger.info("Updated: " + updated + " artifact streams for setting id: " + settingAttribute.getUuid());
          updated = 0;
          COUNTER = 1;
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          found = false;
        }
      }
    }
    // Create Custom Artifact Source folder and migrate all CUSTOM ARTIFACT STREAMS
    migrateCustomArtifactStreamsToAccountLevel(appIdToNameMap, serviceIdToNameMap, bulkWriteOperation);
    logger.info(
        "Migration Completed - move service level artifact streams to connector level for account " + ACCOUNT_ID);
  }

  private void migrateCustomArtifactStreamsToAccountLevel(Map<String, String> appIdToNameMap,
      Map<String, String> serviceIdToNameMap, BulkWriteOperation bulkWriteOperation) {
    logger.info("Inside migrateCustomArtifactStreamsToAccountLevel for account: " + ACCOUNT_ID);
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType("ALL").build();
    Set<String> prodFilterTypes = new HashSet<>();
    prodFilterTypes.add("PROD");
    EnvFilter prodEnvFilter = EnvFilter.builder().filterTypes(prodFilterTypes).build();
    Set<String> nonProdFilterTypes = new HashSet<>();
    nonProdFilterTypes.add("NON_PROD");
    EnvFilter nonProdEnvFilter = EnvFilter.builder().filterTypes(nonProdFilterTypes).build();
    Set<UsageRestrictions.AppEnvRestriction> appEnvRestrictions = new HashSet<>();
    appEnvRestrictions.add(
        UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(prodEnvFilter).build());
    appEnvRestrictions.add(
        UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(nonProdEnvFilter).build());
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute()
            .withName(CUSTOM_ARTIFACT_SERVER)
            .withValue(CustomArtifactServerConfig.builder().accountId(ACCOUNT_ID).build())
            .withAccountId(ACCOUNT_ID)
            .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
            .withUsageRestrictions(UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build())
            .build();

    logger.info("Creating Custom Artifact Server folder for account: " + ACCOUNT_ID);
    SettingAttribute savedSettingAttribute = settingsService.save(settingAttribute);

    logger.info("Finding all artifact streams of type CUSTOM");
    int updated = 0;
    boolean found = false;
    try (HIterator<ArtifactStream> customArtifactStreamsIterator =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStreamKeys.artifactStreamType, ArtifactStreamType.CUSTOM.name())
                                 .field(ArtifactStreamKeys.appId)
                                 .notEqual(GLOBAL_APP_ID)
                                 .filter(ArtifactStreamKeys.accountId, ACCOUNT_ID)
                                 .fetch())) {
      while (customArtifactStreamsIterator.hasNext()) {
        found = true;
        ArtifactStream artifactStream = customArtifactStreamsIterator.next();
        String name = settingAttribute.getName() + SEPARATOR + appIdToNameMap.get(artifactStream.getAppId()) + SEPARATOR
            + serviceIdToNameMap.get(artifactStream.getServiceId()) + SEPARATOR + "AS" + COUNTER++ + SEPARATOR
            + artifactStream.getArtifactStreamType();
        bulkWriteOperation
            .find(wingsPersistence.createQuery(ArtifactStream.class)
                      .filter(ArtifactStreamKeys.uuid, artifactStream.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set",
                new BasicDBObject(ArtifactStreamKeys.name, name)
                    .append(ArtifactStreamKeys.appId, GLOBAL_APP_ID)
                    .append(ArtifactStreamKeys.settingId, savedSettingAttribute.getUuid())
                    .append(ArtifactStreamKeys.serviceId, savedSettingAttribute.getUuid())));
        updated++;

        // update all artifacts belonging to this artifact stream - set settingId to the new connector created above
        int updatedArtifacts = 0;
        boolean foundArtifacts = false;
        final DBCollection artifactCollection = wingsPersistence.getCollection(DEFAULT_STORE, "artifacts");
        BulkWriteOperation artifactsBulkWriteOperation = artifactCollection.initializeUnorderedBulkOperation();
        try (HIterator<Artifact> artifactIterator =
                 new HIterator<>(wingsPersistence.createQuery(Artifact.class)
                                     .filter(ArtifactKeys.artifactStreamType, ArtifactStreamType.CUSTOM.name())
                                     .field(ArtifactKeys.appId)
                                     .notEqual(GLOBAL_APP_ID)
                                     .filter(ArtifactKeys.accountId, ACCOUNT_ID)
                                     .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid())
                                     .fetch())) {
          while (artifactIterator.hasNext()) {
            foundArtifacts = true;
            Artifact artifact = artifactIterator.next();
            artifactsBulkWriteOperation
                .find(wingsPersistence.createQuery(Artifact.class)
                          .filter(ArtifactKeys.uuid, artifact.getUuid())
                          .getQueryObject())
                .updateOne(new BasicDBObject("$set",
                    new BasicDBObject(ArtifactKeys.appId, GLOBAL_APP_ID)
                        .append(ArtifactKeys.settingId, savedSettingAttribute.getUuid())));
            artifactsBulkWriteOperation
                .find(wingsPersistence.createQuery(Artifact.class)
                          .filter(ArtifactKeys.uuid, artifact.getUuid())
                          .getQueryObject())
                .updateOne(new BasicDBObject("$unset", new BasicDBObject(ArtifactKeys.serviceIds, "")));
            updatedArtifacts++;
          }
        }
        if (foundArtifacts) {
          artifactsBulkWriteOperation.execute();
          logger.info("Migrated: " + updatedArtifacts + " custom artifacts belonging to artifactStream: "
              + artifactStream.getUuid() + " to settingId: " + savedSettingAttribute.getUuid());
        }
      }
    }
    if (found) {
      bulkWriteOperation.execute();
      logger.info(
          "Migrated: " + updated + " custom artifact streams to setting id: " + savedSettingAttribute.getUuid());
      COUNTER = 1;
    }
  }
}
