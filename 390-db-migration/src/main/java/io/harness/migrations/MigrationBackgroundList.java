package io.harness.migrations;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.all.AccountNextGenEnabledMigration;
import io.harness.migrations.all.AddAccountIdToActivityCollection;
import io.harness.migrations.all.AddAccountIdToBarrierInstanceCollection;
import io.harness.migrations.all.AddAccountIdToCloudFormationRollBackConfig;
import io.harness.migrations.all.AddAccountIdToCommandCollection;
import io.harness.migrations.all.AddAccountIdToCommandLogs;
import io.harness.migrations.all.AddAccountIdToDeploymentEntities;
import io.harness.migrations.all.AddAccountIdToEntityVersion;
import io.harness.migrations.all.AddAccountIdToExecutionInterruptCollection;
import io.harness.migrations.all.AddAccountIdToInfraDefinition;
import io.harness.migrations.all.AddAccountIdToLogAnalysisRecordsMigration;
import io.harness.migrations.all.AddAccountIdToLogDataRecordsMigration;
import io.harness.migrations.all.AddAccountIdToNewRelicMetricAnalysisRecords;
import io.harness.migrations.all.AddAccountIdToPermitCollection;
import io.harness.migrations.all.AddAccountIdToResourceContraintInstanceCollection;
import io.harness.migrations.all.AddAccountIdToServiceCommands;
import io.harness.migrations.all.AddAccountIdToServiceInstance;
import io.harness.migrations.all.AddAccountIdToServiceTemplates;
import io.harness.migrations.all.AddAccountIdToServiceVariables;
import io.harness.migrations.all.AddAccountIdToStateExecutionInstance;
import io.harness.migrations.all.AddAccountIdToStateMachine;
import io.harness.migrations.all.AddAccountIdToTerraformConfig;
import io.harness.migrations.all.AddAccountIdToTimeSeriesAnalysisRecords;
import io.harness.migrations.all.AddAccountIdToTimeSeriesAnomaliesRecordMigration;
import io.harness.migrations.all.AddAccountIdToTimeSeriesCumulativeSums;
import io.harness.migrations.all.AddAccountIdToTimeSeriesKeyTransaction;
import io.harness.migrations.all.AddAccountIdToTimeSeriesMetricGroupMigration;
import io.harness.migrations.all.AddAccountIdToTimeSeriesMetricTemplatesMigration;
import io.harness.migrations.all.AddAccountIdToTimeSeriesRiskSummary;
import io.harness.migrations.all.AddAccountIdToTimeSeriesTransactionThresholdsMigration;
import io.harness.migrations.all.AddAccountIdToTriggerExecutions;
import io.harness.migrations.all.AddAccountIdToTriggers;
import io.harness.migrations.all.AddAccountIdToWorkflowExecutionBaselines;
import io.harness.migrations.all.AddAccountToCVFeedbackRecordMigration;
import io.harness.migrations.all.AddAccountidToTimeSeriesMLScores;
import io.harness.migrations.all.AddAnalysisStatusMigration;
import io.harness.migrations.all.AddAppManifestName;
import io.harness.migrations.all.AddArtifactIdentityMigration;
import io.harness.migrations.all.AddCeFullTrialLicenseToCurrentAccounts;
import io.harness.migrations.all.AddEnableIteratorsToGovernanceConfig;
import io.harness.migrations.all.AddHarnessOwnedToResourceConstraint;
import io.harness.migrations.all.AddInfraMappingNameToInstanceData;
import io.harness.migrations.all.AddIsDefaultFlagToUserGroup;
import io.harness.migrations.all.AddOrchestrationToWorkflows;
import io.harness.migrations.all.AddStateMachineToWorkflowExecutions;
import io.harness.migrations.all.AddValidUntilToSecretUsageLogs;
import io.harness.migrations.all.AddValidUntilToWorkflowExecution;
import io.harness.migrations.all.AmendCorruptedEncryptedServiceVariable;
import io.harness.migrations.all.ApiKeysSetNameMigration;
import io.harness.migrations.all.CDPaidLicenseToNGMigration;
import io.harness.migrations.all.CEViewsMigration;
import io.harness.migrations.all.CleanUpDirectK8sInfraMappingEncryptedFieldsMigration;
import io.harness.migrations.all.CleanupOrphanInfraMappings;
import io.harness.migrations.all.CleanupOrphanInstances;
import io.harness.migrations.all.CleanupSyncStatusForDeletedEntities;
import io.harness.migrations.all.ConvertHttpHeadersStringTypeToList;
import io.harness.migrations.all.CreateNgPrimaryProfileForExistingAccounts;
import io.harness.migrations.all.CreatePrimiryProfileForAllAccounts;
import io.harness.migrations.all.DefaultExperienceMigration;
import io.harness.migrations.all.DelegateGroupIdentifierMigration;
import io.harness.migrations.all.DelegateNgDetailsToDelegateGroupMigration;
import io.harness.migrations.all.DelegateProfileIdentifierMigration;
import io.harness.migrations.all.DelegateTokenMigration;
import io.harness.migrations.all.DelegatesWithoutGroupMigration;
import io.harness.migrations.all.DelegatesWithoutProfileMigration;
import io.harness.migrations.all.DeleteInvalidArtifactStreams;
import io.harness.migrations.all.DeleteInvalidServiceGuardConfigs;
import io.harness.migrations.all.DeleteOrphanNotificationGroups;
import io.harness.migrations.all.DeleteOrphanPerpetualTaskMigration;
import io.harness.migrations.all.DeleteStaleSlackConfigs;
import io.harness.migrations.all.DeleteStaleThirdPartyApiCallLogsMigration;
import io.harness.migrations.all.DeletedAccountStatusMigration;
import io.harness.migrations.all.DisableServiceGuardsWithDeletedConnectorsMigration;
import io.harness.migrations.all.ExecuteWorkflowRollbackActionMigration;
import io.harness.migrations.all.ExplodeLogMLFeedbackRecordsMigration;
import io.harness.migrations.all.FetchAndSaveAccounts;
import io.harness.migrations.all.FetchAndSaveAccounts2;
import io.harness.migrations.all.FixDuplicatedHarnessGroups;
import io.harness.migrations.all.GCPMarketplaceCustomerMigration;
import io.harness.migrations.all.GcpConfigMultipleDelegateMigration;
import io.harness.migrations.all.HelmStateTimeoutMigration;
import io.harness.migrations.all.InfraMappingToDefinitionMigration;
import io.harness.migrations.all.InitInfraProvisionerCounters;
import io.harness.migrations.all.InitPipelineCounters;
import io.harness.migrations.all.InitServiceCounters;
import io.harness.migrations.all.InitWorkflowCounters;
import io.harness.migrations.all.InstanceSyncPerpetualTaskInfoMigration;
import io.harness.migrations.all.InvalidCVConfigDeletionMigration;
import io.harness.migrations.all.K8sStatesTimeoutMigration;
import io.harness.migrations.all.LicenseExpiryReminderTimeMigration;
import io.harness.migrations.all.LogAnalysisAddExecutionIdMigration;
import io.harness.migrations.all.LogAnalysisBaselineMigration;
import io.harness.migrations.all.MarkSendMailFlagAsTrueInUserGroup;
import io.harness.migrations.all.MigrateLogDataRecordsToGoogle;
import io.harness.migrations.all.MigratePipelineStagesToUseDisableAssertion;
import io.harness.migrations.all.MigrateServiceNowCriteriaInPipelines;
import io.harness.migrations.all.MigrateServiceNowCriteriaInWorkflows;
import io.harness.migrations.all.MigrateTimeSeriesRawDataToGoogle;
import io.harness.migrations.all.MoveDelegateNameToDelegateSelectorsMigration;
import io.harness.migrations.all.NoOpMigration;
import io.harness.migrations.all.RemoveDeletedAppIdsFromUserGroups;
import io.harness.migrations.all.RemoveDeprecatedFieldsFromHarnessUserGroup;
import io.harness.migrations.all.RemoveDuplicateUserGroupNameMigration;
import io.harness.migrations.all.RemoveSupportEmailFromSalesContacts;
import io.harness.migrations.all.ScheduleSegmentPublishJob;
import io.harness.migrations.all.SendInviteUrlForAllUserInvites;
import io.harness.migrations.all.SetAccountIdProvisioners;
import io.harness.migrations.all.SetDefaultTimeOutAndActionForManualInterventionFailureStrategy;
import io.harness.migrations.all.SetDummyTechStackForOldAccounts;
import io.harness.migrations.all.SetEmailToIndividualMemberFlag;
import io.harness.migrations.all.SetLastLoginTimeToAllUsers;
import io.harness.migrations.all.TemplateLibraryYamlMigration;
import io.harness.migrations.all.TerraformIsTemplatizedMigration;
import io.harness.migrations.all.TimeSeriesThresholdsMigration;
import io.harness.migrations.all.UpdateAccountEncryptionClassNames;
import io.harness.migrations.all.UpdateInstanceInfoWithLastArtifactIdMigration;
import io.harness.migrations.all.UpdateStaleDefaultAccountIds;
import io.harness.migrations.all.UpdateWorkflowExecutionAccountId;
import io.harness.migrations.all.UpdateWorkflowExecutionDuration;
import io.harness.migrations.all.WFEAddDeploymentMetaData;
import io.harness.migrations.all.WorkflowExecutionAddCDPageCandidateMigration;
import io.harness.migrations.apppermission.ManageApplicationTemplatePermissionMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
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
        .add(Pair.of(183, BaseMigration.class))
        .add(Pair.of(184, SetDefaultTimeOutAndActionForManualInterventionFailureStrategy.class))
        .add(Pair.of(185, ConvertHttpHeadersStringTypeToList.class))
        .add(Pair.of(186, DeleteOrphanPerpetualTaskMigration.class))
        .add(Pair.of(187, BaseMigration.class))
        .add(Pair.of(188, MoveDelegateNameToDelegateSelectorsMigration.class))
        .add(Pair.of(189, CleanupOrphanInfraMappings.class))
        .add(Pair.of(190, InstanceSyncPerpetualTaskInfoMigration.class))
        .add(Pair.of(191, GcpConfigMultipleDelegateMigration.class))
        .add(Pair.of(192, DelegatesWithoutGroupMigration.class))
        .add(Pair.of(193, CreateNgPrimaryProfileForExistingAccounts.class))
        .add(Pair.of(194, AddEnableIteratorsToGovernanceConfig.class))
        .add(Pair.of(195, DelegateTokenMigration.class))
        .add(Pair.of(196, ExecuteWorkflowRollbackActionMigration.class))
        .add(Pair.of(197, DelegateNgDetailsToDelegateGroupMigration.class))
        .add(Pair.of(198, DefaultExperienceMigration.class))
        .add(Pair.of(199, AddAppManifestName.class))
        .add(Pair.of(200, DelegateProfileIdentifierMigration.class))
        .add(Pair.of(201, DelegateGroupIdentifierMigration.class))
        .add(Pair.of(202, DeleteInvalidArtifactStreams.class))
        .add(Pair.of(203, AccountNextGenEnabledMigration.class))
        .add(Pair.of(204, DeleteOrphanPerpetualTaskMigration.class))
        .add(Pair.of(205, ManageApplicationTemplatePermissionMigration.class))
        .add(Pair.of(206, CDPaidLicenseToNGMigration.class))
        .build();
  }
}
