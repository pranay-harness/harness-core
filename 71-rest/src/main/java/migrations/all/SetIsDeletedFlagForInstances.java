package migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to set the field isDeleted as false for all active instances.
 *
 * @author rktummala on 10/07/18
 */
public class SetIsDeletedFlagForInstances implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SetIsDeletedFlagForInstances.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      logger.info("Start - Setting isDeleted flag for active instances");

      List<Key<Instance>> keyList = wingsPersistence.createQuery(Instance.class).asKeyList();
      Set<String> idSet = keyList.stream().map(instanceKey -> (String) instanceKey.getId()).collect(Collectors.toSet());

      UpdateOperations<Instance> updateOperations = wingsPersistence.createUpdateOperations(Instance.class);
      setUnset(updateOperations, "isDeleted", false);
      setUnset(updateOperations, "deletedAt", 0L);

      Query<Instance> query =
          wingsPersistence.createQuery(Instance.class).field("isDeleted").doesNotExist().field("_id").in(idSet);
      wingsPersistence.update(query, updateOperations);

      logger.info("End - Setting isDeleted flag for active instances");

    } catch (Exception ex) {
      logger.error("Error while setting isDeleted flag for active instances", ex);
    }
  }
}
