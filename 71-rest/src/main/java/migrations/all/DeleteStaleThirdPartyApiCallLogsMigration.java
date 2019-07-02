package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallLogKeys;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressWarnings("deprecation")
public class DeleteStaleThirdPartyApiCallLogsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<String> idsToDelete = new ArrayList<>();

    int deleted = 0;
    try (HIterator<ThirdPartyApiCallLog> iterator =
             new HIterator<>(wingsPersistence.createQuery(ThirdPartyApiCallLog.class, excludeAuthority)
                                 .field(ThirdPartyApiCallLogKeys.validUntil)
                                 .doesNotExist()
                                 .fetch())) {
      while (iterator.hasNext()) {
        final ThirdPartyApiCallLog thirdPartyApiCallLog = iterator.next();
        idsToDelete.add(thirdPartyApiCallLog.getUuid());

        deleted++;
        if (deleted != 0 && idsToDelete.size() % 10000 == 0) {
          wingsPersistence.delete(
              wingsPersistence.createQuery(ThirdPartyApiCallLog.class).field("_id").in(idsToDelete));
          logger.info("deleted: " + deleted);
          idsToDelete.clear();
          sleep(ofMillis(3000));
        }
      }

      if (!idsToDelete.isEmpty()) {
        wingsPersistence.delete(wingsPersistence.createQuery(ThirdPartyApiCallLog.class).field("_id").in(idsToDelete));
        logger.info("deleted: " + deleted);
      }
    }

    logger.info("Complete. Deleted " + deleted + " records.");
  }
}
