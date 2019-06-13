package migrations.all;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.verification.CVConfiguration;

/**
 * Created by Pranjal on 06/13/2019
 */
@Slf4j
public class NonWorkflowCVConfigurationMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(CVConfiguration.class, ReadPref.NORMAL);
    UpdateOperations<CVConfiguration> operations = wingsPersistence.createUpdateOperations(CVConfiguration.class);

    DBCursor cvConfigurationRecords = collection.find();

    logger.info("will go through " + cvConfigurationRecords.size() + " records");
    int updated = 0;
    try {
      while (cvConfigurationRecords.hasNext()) {
        DBObject next = cvConfigurationRecords.next();
        String uuId = (String) next.get("_id");
        // cvconfiguration with isWorkflowConfig as null are the old cv Configurations
        // They need to have this field.
        if (next.get("isWorkflowConfig") == null) {
          operations.set("isWorkflowConfig", false);
          update(uuId, operations);
          updated++;
        }
      }
    } catch (Exception e) {
      logger.error("NonWorkflowCVConfigurationMigration failed", e);
    }
    logger.info("Complete. Updated " + updated + " records.");
  }

  private void update(String uuId, UpdateOperations<CVConfiguration> operations) {
    Query<CVConfiguration> query = wingsPersistence.createQuery(CVConfiguration.class).filter(ID_KEY, uuId);
    wingsPersistence.update(query, operations);
  }
}
