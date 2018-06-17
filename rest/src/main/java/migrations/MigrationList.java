package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddValidUntilToActivity;
import migrations.all.AddValidUntilToAlert;
import migrations.all.AddValidUntilToCommandLog;
import migrations.all.AddValidUntilToDelegateTask;
import migrations.all.EntityNameValidationMigration_All_00;
import migrations.all.LearningEngineTaskGroupNameMigration;
import migrations.all.MetricAnalysisRecordGroupNameMigration;
import migrations.all.MetricDataRecordGroupNameMigration;
import migrations.all.MetricMLAnalysisRecordGroupNameMigration;
import migrations.all.NewRelicMetricDataGroupNameMigration;
import migrations.all.RemoveResizeFromStatefulSetWorkflows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MigrationList {
  /**
   * Add your migrations to the end of the list with the next sequence number. After it has been in production for a few
   * releases it can be deleted, but keep at least one item in this list with the latest sequence number. You can use
   * BaseMigration.class as a placeholder for any removed class.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(141, RemoveResizeFromStatefulSetWorkflows.class))
        .add(Pair.of(142, MetricDataRecordGroupNameMigration.class))
        .add(Pair.of(143, MetricAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(144, MetricMLAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(145, LearningEngineTaskGroupNameMigration.class))
        .add(Pair.of(146, AddValidUntilToAlert.class))
        .add(Pair.of(147, BaseMigration.class))
        .add(Pair.of(148, EntityNameValidationMigration_All_00.class))
        .add(Pair.of(149, NewRelicMetricDataGroupNameMigration.class))
        .add(Pair.of(150, AddValidUntilToDelegateTask.class))
        .add(Pair.of(151, AddValidUntilToActivity.class))
        .add(Pair.of(152, BaseMigration.class))
        .add(Pair.of(153, AddValidUntilToCommandLog.class))
        .build();
  }
}
