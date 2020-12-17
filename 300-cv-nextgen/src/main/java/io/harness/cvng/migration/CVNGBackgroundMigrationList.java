
package io.harness.cvng.migration;

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
        .build();
  }
}
