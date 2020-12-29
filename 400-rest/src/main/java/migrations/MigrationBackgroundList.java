package migrations;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import migrations.all.AddAccountIdToActivityCollection;
import migrations.all.AddAccountIdToBarrierInstanceCollection;
import migrations.all.AddAccountIdToCloudFormationRollBackConfig;
import migrations.all.AddAccountIdToCommandCollection;
import migrations.all.AddAccountIdToCommandLogs;
import migrations.all.AddAccountIdToDeploymentEntities;
import migrations.all.AddAccountIdToEntityVersion;
import migrations.all.AddAccountIdToExecutionInterruptCollection;
import migrations.all.AddAccountIdToInfraDefinition;
import migrations.all.AddAccountIdToLogAnalysisRecordsMigration;
import migrations.all.AddAccountIdToLogDataRecordsMigration;
import migrations.all.AddAccountIdToNewRelicMetricAnalysisRecords;
import migrations.all.AddAccountIdToPermitCollection;
import migrations.all.AddAccountIdToResourceContraintInstanceCollection;
import migrations.all.AddAccountIdToServiceCommands;
import migrations.all.AddAccountIdToServiceInstance;
import migrations.all.AddAccountIdToServiceTemplates;
import migrations.all.AddAccountIdToServiceVariables;
import migrations.all.AddAccountIdToStateExecutionInstance;
import migrations.all.AddAccountIdToStateMachine;
import migrations.all.AddAccountIdToTerraformConfig;
import migrations.all.AddAccountIdToTimeSeriesAnalysisRecords;
import migrations.all.AddAccountIdToTimeSeriesAnomaliesRecordMigration;
import migrations.all.AddAccountIdToTimeSeriesCumulativeSums;
import migrations.all.AddAccountIdToTimeSeriesKeyTransaction;
import migrations.all.AddAccountIdToTimeSeriesMetricGroupMigration;
import migrations.all.AddAccountIdToTimeSeriesMetricTemplatesMigration;
import migrations.all.AddAccountIdToTimeSeriesRiskSummary;
import migrations.all.AddAccountIdToTimeSeriesTransactionThresholdsMigration;
import migrations.all.AddAccountIdToTriggerExecutions;
import migrations.all.AddAccountIdToTriggers;
import migrations.all.AddAccountIdToWorkflowExecutionBaselines;
import migrations.all.AddAccountToCVFeedbackRecordMigration;
import migrations.all.AddAccountidToTimeSeriesMLScores;
import migrations.all.AddAnalysisStatusMigration;
import migrations.all.AddArtifactIdentityMigration;
import migrations.all.AddCeFullTrialLicenseToCurrentAccounts;
import migrations.all.AddHarnessOwnedToResourceConstraint;
import migrations.all.AddInfraMappingNameToInstanceData;
import migrations.all.AddIsDefaultFlagToUserGroup;
import migrations.all.AddOrchestrationToWorkflows;
import migrations.all.AddStateMachineToWorkflowExecutions;
import migrations.all.AddValidUntilToSecretUsageLogs;
import migrations.all.AddValidUntilToWorkflowExecution;
import migrations.all.AmendCorruptedEncryptedServiceVariable;
import migrations.all.ApiKeysSetNameMigration;
import migrations.all.CEViewsMigration;
import migrations.all.CleanUpDirectK8sInfraMappingEncryptedFieldsMigration;
import migrations.all.CleanupOrphanInstances;
import migrations.all.CleanupSyncStatusForDeletedEntities;
import migrations.all.CreatePrimiryProfileForAllAccounts;
import migrations.all.DelegatesWithoutProfileMigration;
import migrations.all.DeleteInvalidServiceGuardConfigs;
import migrations.all.DeleteOrphanNotificationGroups;
import migrations.all.DeleteStaleSlackConfigs;
import migrations.all.DeleteStaleThirdPartyApiCallLogsMigration;
import migrations.all.DeletedAccountStatusMigration;
import migrations.all.DisableServiceGuardsWithDeletedConnectorsMigration;
import migrations.all.ExplodeLogMLFeedbackRecordsMigration;
import migrations.all.FetchAndSaveAccounts;
import migrations.all.FetchAndSaveAccounts2;
import migrations.all.FixDuplicatedHarnessGroups;
import migrations.all.GCPMarketplaceCustomerMigration;
import migrations.all.HelmStateTimeoutMigration;
import migrations.all.InfraMappingToDefinitionMigration;
import migrations.all.InitInfraProvisionerCounters;
import migrations.all.InitPipelineCounters;
import migrations.all.InitServiceCounters;
import migrations.all.InitWorkflowCounters;
import migrations.all.InvalidCVConfigDeletionMigration;
import migrations.all.K8sStatesTimeoutMigration;
import migrations.all.LicenseExpiryReminderTimeMigration;
import migrations.all.LogAnalysisAddExecutionIdMigration;
import migrations.all.LogAnalysisBaselineMigration;
import migrations.all.MarkSendMailFlagAsTrueInUserGroup;
import migrations.all.MigrateLogDataRecordsToGoogle;
import migrations.all.MigratePipelineStagesToUseDisableAssertion;
import migrations.all.MigrateServiceNowCriteriaInPipelines;
import migrations.all.MigrateServiceNowCriteriaInWorkflows;
import migrations.all.MigrateTimeSeriesRawDataToGoogle;
import migrations.all.NoOpMigration;
import migrations.all.RemoveDeletedAppIdsFromUserGroups;
import migrations.all.RemoveDeprecatedFieldsFromHarnessUserGroup;
import migrations.all.RemoveDuplicateUserGroupNameMigration;
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
import migrations.all.UpdateStaleDefaultAccountIds;
import migrations.all.UpdateWorkflowExecutionAccountId;
import migrations.all.UpdateWorkflowExecutionDuration;
import migrations.all.WFEAddDeploymentMetaData;
import migrations.all.WorkflowExecutionAddCDPageCandidateMigration;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class MigrationBackgroundList {
  /**
   * Add your background migrations to the end of the list with the next sequence number.
   * Make sure your background migration is resumable and with rate limit that does not exhaust
   * the resources.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(1, BaseMigration.class))
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
        .add(Pair.of(93, K8sStatesTimeoutMigration.class))
        .add(Pair.of(94, BaseMigration.class))
        .add(Pair.of(95, BaseMigration.class))
        .add(Pair.of(96, BaseMigration.class))
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
        .add(Pair.of(116, AddAccountIdToTimeSeriesMetricGroupMigration.class))
        .add(Pair.of(117, BaseMigration.class))
        .add(Pair.of(118, AddAccountIdToTimeSeriesCumulativeSums.class))
        .add(Pair.of(119, AddAccountIdToTimeSeriesRiskSummary.class))
        .add(Pair.of(120, AddAccountIdToNewRelicMetricAnalysisRecords.class))
        .add(Pair.of(121, BaseMigration.class))
        .add(Pair.of(122, AddAccountIdToWorkflowExecutionBaselines.class))
        .add(Pair.of(123, AddAccountIdToTimeSeriesKeyTransaction.class))
        .add(Pair.of(124, CreatePrimiryProfileForAllAccounts.class))
        .add(Pair.of(125, DisableServiceGuardsWithDeletedConnectorsMigration.class))
        .add(Pair.of(126, DeleteInvalidServiceGuardConfigs.class))
        .add(Pair.of(127, AddAccountIdToTimeSeriesAnomaliesRecordMigration.class))
        .add(Pair.of(128, AddAccountidToTimeSeriesMLScores.class))
        .add(Pair.of(129, CleanUpDirectK8sInfraMappingEncryptedFieldsMigration.class))
        .add(Pair.of(130, InfraMappingToDefinitionMigration.class))
        .add(Pair.of(131, K8sStatesTimeoutMigration.class))
        .add(Pair.of(132, RemoveDuplicateUserGroupNameMigration.class))
        .add(Pair.of(133, MigrateServiceNowCriteriaInPipelines.class))
        .add(Pair.of(134, MigrateServiceNowCriteriaInWorkflows.class))
        .add(Pair.of(135, BaseMigration.class))
        .add(Pair.of(136, BaseMigration.class))
        .add(Pair.of(137, BaseMigration.class))
        .add(Pair.of(138, RemoveDeletedAppIdsFromUserGroups.class))
        .add(Pair.of(139, BaseMigration.class))
        .add(Pair.of(140, AddAccountIdToCommandLogs.class))
        .add(Pair.of(141, BaseMigration.class))
        .add(Pair.of(142, UpdateStaleDefaultAccountIds.class))
        .add(Pair.of(143, BaseMigration.class))
        .add(Pair.of(144, BaseMigration.class))
        .add(Pair.of(145, RemoveDeletedAppIdsFromUserGroups.class))
        .add(Pair.of(146, AddAccountIdToServiceCommands.class))
        .add(Pair.of(147, AddAccountIdToServiceVariables.class))
        .add(Pair.of(148, AddCeFullTrialLicenseToCurrentAccounts.class))
        .add(Pair.of(149, AddAccountIdToServiceInstance.class))
        .add(Pair.of(150, BaseMigration.class))
        .add(Pair.of(151, BaseMigration.class))
        .add(Pair.of(152, BaseMigration.class))
        .add(Pair.of(153, WorkflowExecutionAddCDPageCandidateMigration.class))
        .add(Pair.of(154, AddAccountIdToExecutionInterruptCollection.class))
        .add(Pair.of(155, AddAccountIdToResourceContraintInstanceCollection.class))
        .add(Pair.of(156, AddAccountIdToServiceTemplates.class))
        .add(Pair.of(157, BaseMigration.class))
        .add(Pair.of(158, AddAccountIdToPermitCollection.class))
        .add(Pair.of(159, BaseMigration.class))
        .add(Pair.of(160, RemoveDeprecatedFieldsFromHarnessUserGroup.class))
        .add(Pair.of(161, BaseMigration.class))
        .add(Pair.of(162, BaseMigration.class))
        .add(Pair.of(163, FixDuplicatedHarnessGroups.class))
        .add(Pair.of(164, AddAccountIdToActivityCollection.class))
        .add(Pair.of(165, AddAccountIdToBarrierInstanceCollection.class))
        .add(Pair.of(166, AddAccountIdToEntityVersion.class))
        .add(Pair.of(167, AddAccountIdToTriggerExecutions.class))
        .add(Pair.of(168, AddAccountIdToTriggers.class))
        .add(Pair.of(169, InvalidCVConfigDeletionMigration.class))
        .add(Pair.of(170, BaseMigration.class))
        .add(Pair.of(171, BaseMigration.class))
        .add(Pair.of(172, AddAccountIdToCommandCollection.class))
        .add(Pair.of(173, BaseMigration.class))
        .add(Pair.of(174, InfraMappingToDefinitionMigration.class))
        .add(Pair.of(175, BaseMigration.class))
        .add(Pair.of(176, AddAccountIdToStateExecutionInstance.class))
        .add(Pair.of(177, AddAccountIdToStateMachine.class))
        .add(Pair.of(178, DeletedAccountStatusMigration.class))
        .add(Pair.of(179, LicenseExpiryReminderTimeMigration.class))
        .add(Pair.of(180, BaseMigration.class))
        .add(Pair.of(181, GCPMarketplaceCustomerMigration.class))
        .add(Pair.of(182, CEViewsMigration.class))
        .build();
  }
}
