package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Preference;
import software.wings.dl.WingsPersistence;

@Slf4j
public class PreferenceUserIdRemoveDollarSignMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    Query<Preference> query = wingsPersistence.createQuery(Preference.class);

    try (HIterator<Preference> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Preference preference = records.next();
        String userId = preference.getUserId();
        if (!isEmpty(userId) && userId.endsWith("$")) {
          String newUserId = userId.substring(0, userId.length() - 1);
          preference.setUserId(newUserId);
          wingsPersistence.save(preference);
          logger.info("Updated user Id {} for preference {}", preference.getUserId(), preference.getUuid());
        }
      }
    }
  }
}
