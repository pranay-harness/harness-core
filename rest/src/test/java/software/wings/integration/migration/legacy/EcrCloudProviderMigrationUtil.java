package software.wings.integration.migration.legacy;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;

import java.util.List;

/**
 * Migration script to do the following
 * 1) Find all the ECR Artifact Server / connector and create cloud providers if they don't exist.
 * 2) Change the artifact stream's settingId to cloud provider instead of ECR artifact server.
 * @author rktummala on 08/15/17.
 */
@Integration
@Ignore
public class EcrCloudProviderMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(EcrCloudProviderMigrationUtil.class);

  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void createAwsCloudProvidersFromEcrArtifactServers() {
    logger.info("Creating new AWS cloud providers based on the ECR Artifact Servers");
    try {
      // Get ECR Artifact Servers / Connectors
      DBCursor artifactServerCursor =
          wingsPersistence.getCollection("settingAttributes").find(new BasicDBObject("value.type", "ECR"));
      List<DBObject> artifactServers = artifactServerCursor.toArray();
      for (DBObject artifactServer : artifactServers) {
        DBObject value = (DBObject) artifactServer.get("value");
        String name = (String) artifactServer.get("name");
        String artifactServerId = (String) artifactServer.get("_id");
        String accountId = (String) artifactServer.get("accountId");
        String accessKey = (String) value.get("accessKey");
        String region = (String) value.get("region");
        String secretKey = (String) value.get("secretKey");
        DBObject createdByUser = (DBObject) artifactServer.get("createdBy");
        String userUuid = (String) createdByUser.get("uuid");
        String userName = (String) createdByUser.get("name");
        String userEmail = (String) createdByUser.get("email");

        SimpleEncryption simpleEncryption = new SimpleEncryption(accountId);
        char[] decryptedSecretKeyChars = simpleEncryption.decryptChars(secretKey.toCharArray());
        String decryptedSecretKey = new String(decryptedSecretKeyChars);

        DBCursor cloudProviderCursor = wingsPersistence.getCollection("settingAttributes")
                                           .find(new BasicDBObject("$and",
                                               new BasicDBObject[] {new BasicDBObject("value.type", "AWS"),
                                                   new BasicDBObject("value.accessKey", accessKey),
                                                   new BasicDBObject("value.secretKey", decryptedSecretKey)}));

        String cloudProviderId = null;
        // Check if cloud provider with the same accessKey and secretKey exists
        if (cloudProviderCursor.count() == 0) {
          // If it doesn't exist, create a cloud provider
          AwsConfig awsConfig = new AwsConfig();
          awsConfig.setAccessKey(accessKey);
          awsConfig.setSecretKey(decryptedSecretKeyChars);
          awsConfig.setAccountId(accountId);
          awsConfig.setType("AWS");
          SettingAttribute settingAttribute = new SettingAttribute();
          settingAttribute.setValue(awsConfig);
          settingAttribute.setCategory(Category.CLOUD_PROVIDER);
          settingAttribute.setAccountId(accountId);
          settingAttribute.setAppId(GLOBAL_APP_ID);
          settingAttribute.setEnvId(GLOBAL_ENV_ID);
          settingAttribute.setName(name);
          settingAttribute.setCreatedAt(System.nanoTime());
          settingAttribute.setCreatedBy(EmbeddedUser.builder().email(userEmail).name(userName).uuid(userUuid).build());

          settingAttribute.setUuid(generateUuid());
          try {
            String newId = wingsPersistence.save(settingAttribute);
            cloudProviderId = newId;
          } catch (DuplicateKeyException ex) {
            // If there is a cloud provider with the same
            settingAttribute.setName(name + generateUuid());
            String newId = wingsPersistence.save(settingAttribute);
            cloudProviderId = newId;
          }
        } else {
          // Cloud provider exists
          DBObject cloudProviderObject = cloudProviderCursor.next();
          cloudProviderId = (String) cloudProviderObject.get("_id");
        }

        // Get the ECR artifact streams
        DBCursor artifactStreamCursor =
            wingsPersistence.getCollection("artifactStream").find(new BasicDBObject("settingId", artifactServerId));
        List<DBObject> artifactStreamObjects = artifactStreamCursor.toArray();
        for (DBObject artifactStreamObject : artifactStreamObjects) {
          String appId = (String) artifactStreamObject.get("appId");
          String artifactStreamId = (String) artifactStreamObject.get("_id");
          EcrArtifactStream artifactStream = (EcrArtifactStream) artifactStreamService.get(appId, artifactStreamId);
          artifactStream.setSettingId(cloudProviderId);
          artifactStream.setRegion(region);
          wingsPersistence.save(artifactStream);
        }
      }
    } catch (Exception e) {
      logger.info("Creating cloud provider failed");
      logger.error("", e);
    }
    logger.info("Creating cloud provider completed");
  }
}
