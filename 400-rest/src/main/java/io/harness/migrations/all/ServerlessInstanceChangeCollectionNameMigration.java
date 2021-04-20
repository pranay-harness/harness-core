package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.DBCollection;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class ServerlessInstanceChangeCollectionNameMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection sereverlessInstanceCollectionWithHyphen =
        wingsPersistence.getCollection(DEFAULT_STORE, "serverless-instance");

    boolean doesDataBaseContainServerlessInstance =
        sereverlessInstanceCollectionWithHyphen.getDB().getCollectionNames().contains("serverless-instance");

    if (doesDataBaseContainServerlessInstance) {
      sereverlessInstanceCollectionWithHyphen.rename("serverlessInstance", true);
    }
  }
}
