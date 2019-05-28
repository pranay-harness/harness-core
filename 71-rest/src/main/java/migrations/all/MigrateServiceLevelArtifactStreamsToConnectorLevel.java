package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MigrateServiceLevelArtifactStreamsToConnectorLevel implements Migration {
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w"; // TODO: change this to reflect correct account
  private static final String SEPARATOR = "_";
  private static int COUNTER = 1;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
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

    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "artifactStream");
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
    logger.info(
        "Migration Completed - move service level artifact streams to connector level for account " + ACCOUNT_ID);
  }
}
