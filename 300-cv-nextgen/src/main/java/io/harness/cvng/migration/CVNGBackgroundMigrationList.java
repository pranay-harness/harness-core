
package io.harness.cvng.migration;

import io.harness.cvng.migration.list.AddMonitoringSourcesToVerificationJobMigration;
import io.harness.cvng.migration.list.CVNGBaseMigration;
import io.harness.cvng.migration.list.RecreateMetricPackAndThresholdMigration;
import io.harness.cvng.migration.list.UpdateActivityStatusMigration;
import io.harness.cvng.migration.list.UpdateRiskIntToRiskEnum;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class CVNGBackgroundMigrationList {
  /**
   * Add your background migrations to the end of the list with the next sequence number.
   * Make sure your background migration is resumable and with rate limit that does not exhaust
   * the resources.
   */
  public static List<Pair<Integer, Class<? extends CNVGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends CNVGMigration>>>()
        .add(Pair.of(1, CVNGBaseMigration.class))
        .add(Pair.of(2, RecreateMetricPackAndThresholdMigration.class))
        .add(Pair.of(3, UpdateActivityStatusMigration.class))
        .add(Pair.of(4, AddMonitoringSourcesToVerificationJobMigration.class))
        .add(Pair.of(5, UpdateRiskIntToRiskEnum.class))
        .build();
  }
}
