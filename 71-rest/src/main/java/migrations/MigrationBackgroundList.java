package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddValidUntilToCommandLog;
import migrations.all.NewRelicMetricDataBackupMigration;
import migrations.all.RemoveSupportEmailFromSalesContacts;
import migrations.all.SetLastLoginTimeToAllUsers;
import migrations.all.TerraformIsTemplatizedMigration;
import migrations.all.TimeSeriesRiskSummaryMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MigrationBackgroundList {
  /**
   * Add your background migrations to the end of the list with the next sequence number.
   * Make sure your background migration is resumable and with rate limit that does not exhaust
   * the resources.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(1, AddValidUntilToCommandLog.class))
        .add(Pair.of(2, BaseMigration.class))
        .add(Pair.of(3, SetLastLoginTimeToAllUsers.class))
        .add(Pair.of(4, BaseMigration.class))
        .add(Pair.of(5, BaseMigration.class))
        .add(Pair.of(6, BaseMigration.class))
        .add(Pair.of(7, RemoveSupportEmailFromSalesContacts.class))
        .add(Pair.of(8, NewRelicMetricDataBackupMigration.class))
        .add(Pair.of(9, TimeSeriesRiskSummaryMigration.class))
        .add(Pair.of(10, TerraformIsTemplatizedMigration.class))
        .build();
  }
}
