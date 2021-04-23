package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Preference;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

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
          log.info("Updated user Id {} for preference {}", preference.getUserId(), preference.getUuid());
        }
      }
    }
  }
}
