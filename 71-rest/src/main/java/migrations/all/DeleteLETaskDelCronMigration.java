package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

public class DeleteLETaskDelCronMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection("quartz_verification_triggers");
    collection.findAndRemove(new BasicDBObject("keyName", "LEARNING_ENGINE_TASK_QUEUE_DEL_CRON"));
  }
}
