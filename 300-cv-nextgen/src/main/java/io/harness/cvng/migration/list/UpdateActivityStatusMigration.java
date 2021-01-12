package io.harness.cvng.migration.list;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.migration.CNVGMigration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class UpdateActivityStatusMigration implements CNVGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;

  @Override
  public void migrate() {
    Instant now = Instant.now();
    log.info("Begin migration for updating status of activities");
    Query<Activity> activityQuery = hPersistence.createQuery(Activity.class)
                                        .field(ActivityKeys.activityStartTime)
                                        .lessThan(now.minus(Duration.ofHours(2)));

    try (HIterator<Activity> iterator = new HIterator<>(activityQuery.fetch())) {
      while (iterator.hasNext()) {
        try {
          Activity activity = iterator.next();
          activityService.updateActivityStatus(activity);
        } catch (Exception ex) {
          log.error("Exception occurred while updating status of activity", ex);
        }
      }
    }
  }
}
