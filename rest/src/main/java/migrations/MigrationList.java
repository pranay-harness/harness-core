package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddUsersToAdminUserGroup;
import migrations.all.AddValidUntilToAlert;
import migrations.all.AddValidUntilToDelegateTask;
import migrations.all.EntityNameValidationMigration_All_00;
import migrations.all.FixInstanceData;
import migrations.all.FixInstanceDataForAwsSSH;
import migrations.all.GitSyncToAllAccounts;
import migrations.all.LearningEngineTaskGroupNameMigration;
import migrations.all.LogAnalysisExperimentalRecordsMigration;
import migrations.all.MetricAnalysisRecordGroupNameMigration;
import migrations.all.MetricDataRecordGroupNameMigration;
import migrations.all.MetricMLAnalysisRecordGroupNameMigration;
import migrations.all.NewRelicMetricAnalysisRecordsMigration;
import migrations.all.NewRelicMetricDataRecordsMigration;
import migrations.all.RemoveResizeFromStatefulSetWorkflows;
import migrations.all.RenameProvisionNodeToInfrastructureNodeWorkflows;
import migrations.all.SetRollbackFlagToWorkflows;
import migrations.all.TimeSeriesAnalysisRecordsMigration;
import migrations.all.TimeSeriesMLScoresMigration;
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
        .add(Pair.of(130, FixInstanceData.class))
        .add(Pair.of(131, LogAnalysisExperimentalRecordsMigration.class))
        .add(Pair.of(132, FixInstanceDataForAwsSSH.class))
        .add(Pair.of(133, NewRelicMetricAnalysisRecordsMigration.class))
        .add(Pair.of(134, NewRelicMetricDataRecordsMigration.class))
        .add(Pair.of(135, TimeSeriesAnalysisRecordsMigration.class))
        .add(Pair.of(136, TimeSeriesMLScoresMigration.class))
        .add(Pair.of(137, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(138, AddUsersToAdminUserGroup.class))
        .add(Pair.of(139, RenameProvisionNodeToInfrastructureNodeWorkflows.class))
        .add(Pair.of(140, GitSyncToAllAccounts.class))
        .add(Pair.of(141, RemoveResizeFromStatefulSetWorkflows.class))
        .add(Pair.of(142, MetricDataRecordGroupNameMigration.class))
        .add(Pair.of(143, MetricAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(144, MetricMLAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(145, LearningEngineTaskGroupNameMigration.class))
        .add(Pair.of(146, AddValidUntilToAlert.class))
        .add(Pair.of(147, AddValidUntilToDelegateTask.class))
        .add(Pair.of(148, EntityNameValidationMigration_All_00.class))
        .build();
  }
}
