package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.beans.infrastructure.TerraformConfig.TerraformConfigKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class AddAccountIdToTerraformConfig implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "terraformConfig");
    logger.info("Adding accountId to terraformConfig");
    try (HIterator<Application> applicationHIterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class, excludeAuthority).fetch())) {
      while (applicationHIterator.hasNext()) {
        Application application = applicationHIterator.next();
        logger.info("Adding accountId to terraformConfig for application {}", application.getUuid());
        final WriteResult result = collection.updateMulti(
            new BasicDBObject(TerraformConfigKeys.appId, application.getUuid())
                .append(TerraformConfigKeys.accountId, null),
            new BasicDBObject("$set", new BasicDBObject(TerraformConfigKeys.accountId, application.getAccountId())));
        logger.info("updated {} terraformConfigs for application {} ", result.getN(), application.getUuid());
      }
    }
    logger.info("Adding accountIds to terraformConfig completed for all applications");
  }
}
