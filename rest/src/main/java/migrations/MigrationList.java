package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddReplCtrlNameToKubeSetupProperties;
import migrations.all.AddUsersToAdminUserGroup;
import migrations.all.AddValidUntilToActivity;
import migrations.all.AddValidUntilToAlert;
import migrations.all.AddValidUntilToDelegateConnectionResults;
import migrations.all.AddValidUntilToDelegateTask;
import migrations.all.AppKeywordsMigration;
import migrations.all.AuthTokenTtlMigration;
import migrations.all.DeleteOldThirdPartyApiCallsMigration;
import migrations.all.DeleteStaleSecretUsageLogs;
import migrations.all.DeleteStaleYamlChangeSetsMigration;
import migrations.all.DirectKubernetesOrphanRemoval;
import migrations.all.DirectKubernetesOrphanRemovalMirgation;
import migrations.all.DropDelegateScopeCollectionMigration;
import migrations.all.DropOldCollectionMigration;
import migrations.all.DropStringCollectionMigration;
import migrations.all.DropYamlGitSyncCollectionMigration;
import migrations.all.EntityNameValidationMigration_All_00;
import migrations.all.EntityNameValidationMigration_All_01;
import migrations.all.EntityNameValidationMigration_All_02;
import migrations.all.EntityNameValidationMigration_All_03;
import migrations.all.EntityNameValidationMigration_All_04;
import migrations.all.FixInstanceData;
import migrations.all.FixInstanceDataForAwsSSH;
import migrations.all.GitSyncToAllAccounts;
import migrations.all.LearningEngineTaskGroupNameMigration;
import migrations.all.LogAnalysisExperimentalRecordsMigration;
import migrations.all.MetricAnalysisRecordGroupNameMigration;
import migrations.all.MetricDataRecordGroupNameMigration;
import migrations.all.MetricMLAnalysisRecordGroupNameMigration;
import migrations.all.NewRelicMetricAnalysisRecordsMigration;
import migrations.all.NewRelicMetricDataGroupNameMigration;
import migrations.all.NewRelicMetricDataRecordsMigration;
import migrations.all.PreferenceUserIdRemoveDollarSignMigration;
import migrations.all.RemoveResizeFromStatefulSetWorkflows;
import migrations.all.RenameProvisionNodeToInfrastructureNodeWorkflows;
import migrations.all.SetRollbackFlagToWorkflows;
import migrations.all.SystemTemplateGalleryMigration;
import migrations.all.TimeSeriesAnalysisRecordsMigration;
import migrations.all.TimeSeriesMLScoresMigration;
import migrations.all.UnregisteredUserNameMigration;
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
        .add(Pair.of(147, BaseMigration.class))
        .add(Pair.of(148, EntityNameValidationMigration_All_00.class))
        .add(Pair.of(149, NewRelicMetricDataGroupNameMigration.class))
        .add(Pair.of(150, AddValidUntilToDelegateTask.class))
        .add(Pair.of(151, AddValidUntilToActivity.class))
        .add(Pair.of(152, BaseMigration.class))
        .add(Pair.of(153, BaseMigration.class))
        .add(Pair.of(154, EntityNameValidationMigration_All_01.class))
        .add(Pair.of(155, BaseMigration.class))
        .add(Pair.of(156, BaseMigration.class))
        .add(Pair.of(157, BaseMigration.class))
        .add(Pair.of(158, EntityNameValidationMigration_All_02.class))
        .add(Pair.of(159, BaseMigration.class))
        .add(Pair.of(160, EntityNameValidationMigration_All_03.class))
        .add(Pair.of(161, DropStringCollectionMigration.class))
        .add(Pair.of(162, DropDelegateScopeCollectionMigration.class))
        .add(Pair.of(163, PreferenceUserIdRemoveDollarSignMigration.class))
        .add(Pair.of(164, DirectKubernetesOrphanRemovalMirgation.class))
        .add(Pair.of(165, DropYamlGitSyncCollectionMigration.class))
        .add(Pair.of(166, DirectKubernetesOrphanRemoval.class))
        .add(Pair.of(167, AddReplCtrlNameToKubeSetupProperties.class))
        .add(Pair.of(168, BaseMigration.class))
        .add(Pair.of(169, AddValidUntilToDelegateConnectionResults.class))
        .add(Pair.of(170, EntityNameValidationMigration_All_04.class))
        .add(Pair.of(171, DropOldCollectionMigration.class))
        .add(Pair.of(172, SystemTemplateGalleryMigration.class))
        .add(Pair.of(173, SystemTemplateGalleryMigration.class))
        .add(Pair.of(174, AppKeywordsMigration.class))
        .add(Pair.of(175, DeleteOldThirdPartyApiCallsMigration.class))
        .add(Pair.of(176, UnregisteredUserNameMigration.class))
        .add(Pair.of(177, DeleteStaleYamlChangeSetsMigration.class))
        .add(Pair.of(178, AuthTokenTtlMigration.class))
        .add(Pair.of(179, DeleteStaleSecretUsageLogs.class))
        .build();
  }
}
