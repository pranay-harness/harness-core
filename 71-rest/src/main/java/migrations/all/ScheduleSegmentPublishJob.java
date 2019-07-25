package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public class ScheduleSegmentPublishJob implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      // delete entries if any
      wingsPersistence.delete(wingsPersistence.createQuery(SegmentGroupEventJobContext.class));

      // schedule new entry
      final Instant nextIteration = Instant.now().plus(2, ChronoUnit.MINUTES);
      SegmentGroupEventJobContext jobContext = new SegmentGroupEventJobContext(nextIteration.toEpochMilli());
      wingsPersistence.save(jobContext);
    } catch (Exception e) {
      logger.error("Exception scheduling segment job", e);
    }
  }
}
