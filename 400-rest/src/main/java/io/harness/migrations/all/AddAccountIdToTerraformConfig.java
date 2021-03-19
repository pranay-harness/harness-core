package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.infrastructure.TerraformConfig.TerraformConfigKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
@OwnedBy(CDP)
public class AddAccountIdToTerraformConfig implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "terraformConfig");
    log.info("Adding accountId to terraformConfig");
    try (HIterator<Application> applicationHIterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class, excludeAuthority).fetch())) {
      while (applicationHIterator.hasNext()) {
        Application application = applicationHIterator.next();
        log.info("Adding accountId to terraformConfig for application {}", application.getUuid());
        final WriteResult result = collection.updateMulti(
            new BasicDBObject(TerraformConfigKeys.appId, application.getUuid())
                .append(TerraformConfigKeys.accountId, null),
            new BasicDBObject("$set", new BasicDBObject(TerraformConfigKeys.accountId, application.getAccountId())));
        log.info("updated {} terraformConfigs for application {} ", result.getN(), application.getUuid());
      }
    }
    log.info("Adding accountIds to terraformConfig completed for all applications");
  }
}
