package migrations;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;
import migrations.all.AddAccountIdToCloudFormationRollBackConfig;
import migrations.all.AddAccountIdToDeploymentEntities;
import migrations.all.AddAccountIdToInfraDefinition;
import migrations.all.AddAccountIdToLogAnalysisRecordsMigration;
import migrations.all.AddAccountIdToLogDataRecordsMigration;
import migrations.all.AddAccountIdToTerraformConfig;
import migrations.all.AddAccountIdToTimeSeriesAnalysisRecords;
import migrations.all.AddAccountIdToTimeSeriesMetricTemplatesMigration;
import migrations.all.AddAccountIdToTimeSeriesTransactionThresholdsMigration;
import migrations.all.AddAccountToCVFeedbackRecordMigration;
import migrations.all.AddAnalysisStatusMigration;
import migrations.all.AddArtifactIdentityMigration;
import migrations.all.AddCommitTimeToGitSyncError;
import migrations.all.AddHarnessOwnedToResourceConstraint;
import migrations.all.AddInfraMappingNameToInstanceData;
import migrations.all.AddIsDefaultFlagToUserGroup;
import migrations.all.AddOrchestrationToWorkflows;
import migrations.all.AddStateMachineToWorkflowExecutions;
import migrations.all.AddValidUntilToCommandLog;
import migrations.all.AddValidUntilToSecretUsageLogs;
import migrations.all.AddValidUntilToWorkflowExecution;
import migrations.all.AmendCorruptedEncryptedServiceVariable;
import migrations.all.ApiKeysSetNameMigration;
import migrations.all.CleanupOrphanInstances;
import migrations.all.CleanupSyncStatusForDeletedEntities;
import migrations.all.DelegatesWithoutProfileMigration;
import migrations.all.DeleteOrphanNotificationGroups;
import migrations.all.DeleteStaleSlackConfigs;
import migrations.all.DeleteStaleThirdPartyApiCallLogsMigration;
import migrations.all.ExplodeLogMLFeedbackRecordsMigration;
import migrations.all.FetchAndSaveAccounts;
import migrations.all.FetchAndSaveAccounts2;
import migrations.all.GitSyncErrorGitDetailsMigration;
import migrations.all.HelmStateTimeoutMigration;
import migrations.all.InfraMappingToDefinitionMigration;
import migrations.all.InitInfraProvisionerCounters;
import migrations.all.InitPipelineCounters;
import migrations.all.InitServiceCounters;
import migrations.all.InitWorkflowCounters;
import migrations.all.K8sBGTimeoutMigration;
import migrations.all.LogAnalysisAddExecutionIdMigration;
import migrations.all.LogAnalysisBaselineMigration;
import migrations.all.MarkSendMailFlagAsTrueInUserGroup;
import migrations.all.MigrateLogDataRecordsToGoogle;
import migrations.all.MigratePipelineStagesToUseDisableAssertion;
import migrations.all.MigrateTimeSeriesRawDataToGoogle;
import migrations.all.NoOpMigration;
import migrations.all.RemoveSupportEmailFromSalesContacts;
import migrations.all.ScheduleSegmentPublishJob;
import migrations.all.SendInviteUrlForAllUserInvites;
import migrations.all.SetAccountIdProvisioners;
import migrations.all.SetDummyTechStackForOldAccounts;
import migrations.all.SetEmailToIndividualMemberFlag;
import migrations.all.SetLastLoginTimeToAllUsers;
import migrations.all.TemplateLibraryYamlMigration;
import migrations.all.TerraformIsTemplatizedMigration;
import migrations.all.TimeSeriesThresholdsMigration;
import migrations.all.UpdateAccountEncryptionClassNames;
import migrations.all.UpdateInstanceInfoWithLastArtifactIdMigration;
import migrations.all.UpdateWorkflowExecutionAccountId;
import migrations.all.UpdateWorkflowExecutionDuration;
import migrations.all.WFEAddDeploymentMetaData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
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
        .add(Pair.of(8, BaseMigration.class))
        .add(Pair.of(9, BaseMigration.class))
        .add(Pair.of(10, BaseMigration.class))
        .add(Pair.of(11, BaseMigration.class))
        .add(Pair.of(12, TerraformIsTemplatizedMigration.class))
        .add(Pair.of(13, AddValidUntilToWorkflowExecution.class))
        .add(Pair.of(14, SendInviteUrlForAllUserInvites.class))
        .add(Pair.of(15, BaseMigration.class))
        .add(Pair.of(16, AddOrchestrationToWorkflows.class))
        .add(Pair.of(17, CleanupOrphanInstances.class))
        .add(Pair.of(18, CleanupSyncStatusForDeletedEntities.class))
        .add(Pair.of(19, AddStateMachineToWorkflowExecutions.class))
        .add(Pair.of(20, DeleteOrphanNotificationGroups.class))
        .add(Pair.of(21, AddIsDefaultFlagToUserGroup.class))
        .add(Pair.of(22, AddInfraMappingNameToInstanceData.class))
        .add(Pair.of(23, MigrateLogDataRecordsToGoogle.class))
        .add(Pair.of(24, SetEmailToIndividualMemberFlag.class))
        .add(Pair.of(25, SetEmailToIndividualMemberFlag.class))
        .add(Pair.of(26, MarkSendMailFlagAsTrueInUserGroup.class))
        .add(Pair.of(27, AddAnalysisStatusMigration.class))
        .add(Pair.of(28, ExplodeLogMLFeedbackRecordsMigration.class))
        .add(Pair.of(29, InitWorkflowCounters.class))
        .add(Pair.of(30, InitPipelineCounters.class))
        .add(Pair.of(31, InitServiceCounters.class))
        .add(Pair.of(32, SetDummyTechStackForOldAccounts.class))
        .add(Pair.of(33, LogAnalysisAddExecutionIdMigration.class))
        .add(Pair.of(34, LogAnalysisBaselineMigration.class))
        .add(Pair.of(35, InitInfraProvisionerCounters.class))
        .add(Pair.of(36, DeleteStaleThirdPartyApiCallLogsMigration.class))
        .add(Pair.of(37, AddAccountToCVFeedbackRecordMigration.class))
        .add(Pair.of(38, MigrateTimeSeriesRawDataToGoogle.class))
        .add(Pair.of(39, BaseMigration.class))
        .add(Pair.of(40, UpdateWorkflowExecutionDuration.class))
        .add(Pair.of(41, BaseMigration.class))
        .add(Pair.of(42, BaseMigration.class))
        .add(Pair.of(43, FetchAndSaveAccounts.class))
        .add(Pair.of(44, NoOpMigration.class))
        .add(Pair.of(45, FetchAndSaveAccounts2.class))
        .add(Pair.of(46, ScheduleSegmentPublishJob.class))
        .add(Pair.of(47, UpdateWorkflowExecutionAccountId.class))
        .add(Pair.of(48, UpdateAccountEncryptionClassNames.class))
        .add(Pair.of(49, ScheduleSegmentPublishJob.class))
        .add(Pair.of(50, BaseMigration.class))
        .add(Pair.of(51, BaseMigration.class))
        .add(Pair.of(52, ApiKeysSetNameMigration.class))
        .add(Pair.of(53, DeleteStaleSlackConfigs.class))
        .add(Pair.of(54, BaseMigration.class))
        .add(Pair.of(55, BaseMigration.class))
        .add(Pair.of(56, BaseMigration.class))
        .add(Pair.of(57, BaseMigration.class))
        .add(Pair.of(58, MigrateTimeSeriesRawDataToGoogle.class))
        .add(Pair.of(59, BaseMigration.class))
        .add(Pair.of(60, BaseMigration.class))
        .add(Pair.of(61, BaseMigration.class))
        .add(Pair.of(62, BaseMigration.class))
        .add(Pair.of(63, BaseMigration.class))
        .add(Pair.of(64, MigratePipelineStagesToUseDisableAssertion.class))
        .add(Pair.of(65, BaseMigration.class))
        .add(Pair.of(66, BaseMigration.class))
        .add(Pair.of(67, BaseMigration.class))
        .add(Pair.of(68, BaseMigration.class))
        .add(Pair.of(69, BaseMigration.class))
        .add(Pair.of(70, BaseMigration.class))
        .add(Pair.of(71, BaseMigration.class))
        .add(Pair.of(72, BaseMigration.class))
        .add(Pair.of(73, BaseMigration.class))
        .add(Pair.of(74, BaseMigration.class))
        .add(Pair.of(75, BaseMigration.class))
        .add(Pair.of(76, TimeSeriesThresholdsMigration.class))
        .add(Pair.of(77, BaseMigration.class))
        .add(Pair.of(78, BaseMigration.class))
        .add(Pair.of(79, BaseMigration.class))
        .add(Pair.of(80, BaseMigration.class))
        .add(Pair.of(81, AmendCorruptedEncryptedServiceVariable.class))
        .add(Pair.of(82, AddArtifactIdentityMigration.class))
        .add(Pair.of(83, AddHarnessOwnedToResourceConstraint.class))
        .add(Pair.of(84, BaseMigration.class))
        .add(Pair.of(85, WFEAddDeploymentMetaData.class))
        .add(Pair.of(86, SetAccountIdProvisioners.class))
        .add(Pair.of(87, InfraMappingToDefinitionMigration.class))
        .add(Pair.of(88, TemplateLibraryYamlMigration.class))
        .add(Pair.of(89, HelmStateTimeoutMigration.class))
        .add(Pair.of(90, BaseMigration.class))
        .add(Pair.of(91, BaseMigration.class))
        .add(Pair.of(92, UpdateInstanceInfoWithLastArtifactIdMigration.class))
        .add(Pair.of(93, K8sBGTimeoutMigration.class))
        .add(Pair.of(94, BaseMigration.class))
        .add(Pair.of(95, GitSyncErrorGitDetailsMigration.class))
        .add(Pair.of(96, AddCommitTimeToGitSyncError.class))
        .add(Pair.of(97, BaseMigration.class))
        .add(Pair.of(98, BaseMigration.class))
        .add(Pair.of(99, BaseMigration.class))
        .add(Pair.of(100, BaseMigration.class))
        .add(Pair.of(101, BaseMigration.class))
        .add(Pair.of(102, AddAccountIdToLogAnalysisRecordsMigration.class))
        .add(Pair.of(103, AddAccountIdToLogDataRecordsMigration.class))
        .add(Pair.of(104, BaseMigration.class))
        .add(Pair.of(105, AddAccountIdToDeploymentEntities.class))
        .add(Pair.of(106, AddAccountIdToTerraformConfig.class))
        .add(Pair.of(107, AddAccountIdToCloudFormationRollBackConfig.class))
        .add(Pair.of(108, AddAccountIdToInfraDefinition.class))
        .add(Pair.of(109, AddAccountIdToTimeSeriesAnalysisRecords.class))
        .add(Pair.of(110, DelegatesWithoutProfileMigration.class))
        .add(Pair.of(111, AddValidUntilToSecretUsageLogs.class))
        .add(Pair.of(112, AddAccountIdToTimeSeriesTransactionThresholdsMigration.class))
        .add(Pair.of(113, AddAccountIdToLogDataRecordsMigration.class))
        .add(Pair.of(114, BaseMigration.class))
        .add(Pair.of(115, AddAccountIdToTimeSeriesMetricTemplatesMigration.class))
        .build();
  }
}