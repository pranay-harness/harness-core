package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.DBCollection;
import migrations.Migration;

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
