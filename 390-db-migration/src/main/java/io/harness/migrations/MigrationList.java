
package io.harness.migrations;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.accountpermission.AddManageTagsPermission;
import io.harness.migrations.accountpermission.AlertNotificationAccountPermissionMigration;
import io.harness.migrations.accountpermission.CreateCustomDashboardPermissionMigration;
import io.harness.migrations.accountpermission.ManageAccountDefaultsPermissionMigration;
import io.harness.migrations.accountpermission.ManageApiKeyPermissionMigration;
import io.harness.migrations.accountpermission.ManageApplicationsPermissionMigration;
import io.harness.migrations.accountpermission.ManageAuthenticationSettingsPermissionMigration;
import io.harness.migrations.accountpermission.ManageCloudProvidersAndConnectorsPermissionMigration;
import io.harness.migrations.accountpermission.ManageConfigAsCodePermissionMigration;
import io.harness.migrations.accountpermission.ManageCustomDashboardPermissionMigration;
import io.harness.migrations.accountpermission.ManageDelegatePermissionMigration;
import io.harness.migrations.accountpermission.ManageDelegateProfilesPermissionMigration;
import io.harness.migrations.accountpermission.ManageDeploymentFreezePermissionMigration;
import io.harness.migrations.accountpermission.ManageIPWhitelistPermissionMigration;
import io.harness.migrations.accountpermission.ManagePipelineGovernancePermissionMigration;
import io.harness.migrations.accountpermission.ManageTagsMigration;
import io.harness.migrations.accountpermission.RemoveDeprecatedApplicationsCreatePermission;
import io.harness.migrations.accountpermission.RemoveDeprecatedTagManagementPermission;
import io.harness.migrations.all.AddAccountIdToAppEntities;
import io.harness.migrations.all.AddAccountIdToArtifactStreamsMigration;
import io.harness.migrations.all.AddAccountIdToArtifactsMigration;
import io.harness.migrations.all.AddCEPermissionToAllUserGroups;
import io.harness.migrations.all.AddDisabledFieldMigration;
import io.harness.migrations.all.AddDuplicateFieldToCVFeedbacks;
import io.harness.migrations.all.AddEnableIteratorsToTriggers;
import io.harness.migrations.all.AddHarnessCommandLibraryToAccount;
import io.harness.migrations.all.AddHarnessGroupAccessFlagToAccount;
import io.harness.migrations.all.AddInstanceStatsCollectionJobToAllAccounts;
import io.harness.migrations.all.AddLimitVicinityCheckJobToAllAccounts;
import io.harness.migrations.all.AddLoginSettingsToAccountMigration;
import io.harness.migrations.all.AddNgFieldToDelegateGroupMigration;
import io.harness.migrations.all.AddReplCtrlNameToKubeSetupProperties;
import io.harness.migrations.all.AddRestrictionsToSecretManagerConfig;
import io.harness.migrations.all.AddScopedToAccountAttributeToEncryptedData;
import io.harness.migrations.all.AddTagManagementPermissionToAdminUserGroup;
import io.harness.migrations.all.AddTemplateMgmtPermissionToAdminUserGroup;
import io.harness.migrations.all.AddValidUntilToActivity;
import io.harness.migrations.all.AddValidUntilToAlert;
import io.harness.migrations.all.AddValidUntilToDelegate;
import io.harness.migrations.all.AddValidUntilToDelegateTask;
import io.harness.migrations.all.AddWhitelistedDomainsToAccountMigration;
import io.harness.migrations.all.AmiDeploymentTypeMigration;
import io.harness.migrations.all.AppDTemplateMigration;
import io.harness.migrations.all.AppKeywordsMigration;
import io.harness.migrations.all.ArtifactStreamIteratorRequiredMigration;
import io.harness.migrations.all.AuditRecordMigration;
import io.harness.migrations.all.AuditViewerPermissionMigration;
import io.harness.migrations.all.AuthTokenTtlMigration;
import io.harness.migrations.all.AwsAmiAsgDesiredInstancesMigration;
import io.harness.migrations.all.AwsConfigEc2IamRoleMigration;
import io.harness.migrations.all.CECloudAccountMigration;
import io.harness.migrations.all.CVCollectionCronFrequencyMigration;
import io.harness.migrations.all.CleanUpDatadogCallLogMigration;
import io.harness.migrations.all.CleanupOrphanInstances;
import io.harness.migrations.all.CleanupSyncStatusForDeletedEntities;
import io.harness.migrations.all.CloudWatchCVMigration;
import io.harness.migrations.all.ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes;
import io.harness.migrations.all.CreateDefaultAlertNotificationRule;
import io.harness.migrations.all.DanglingAppEnvReferenceRemovalMigration;
import io.harness.migrations.all.DanglingUserInviteCleanupMigration;
import io.harness.migrations.all.DataDogLogCvConfigMigration;
import io.harness.migrations.all.DatadogCVServiceConfigurationMigration;
import io.harness.migrations.all.DatadogCustomMetricMigration;
import io.harness.migrations.all.DeleteCVAlertsMigration;
import io.harness.migrations.all.DeleteCVCronMigration;
import io.harness.migrations.all.DeleteGitActivityWithoutProcCommitIdMigration;
import io.harness.migrations.all.DeleteLETaskDelCronMigration;
import io.harness.migrations.all.DeleteNewRelicMetricNameCronMigration;
import io.harness.migrations.all.DeleteOldThirdPartyApiCallsMigration;
import io.harness.migrations.all.DeleteServiceGuardAlertMigration;
import io.harness.migrations.all.DeleteStaleSecretUsageLogs;
import io.harness.migrations.all.DeleteStaleYamlChangeSetsMigration;
import io.harness.migrations.all.DisableAddingServiceVarsToEcsSpecFFMigration;
import io.harness.migrations.all.DisableWinrmVariablesFFMigration;
import io.harness.migrations.all.DropAppIdIndexOnCommandLogs;
import io.harness.migrations.all.DropDelegateScopeCollectionMigration;
import io.harness.migrations.all.DropExistingIndexForGitFileActivityMigration;
import io.harness.migrations.all.DropMongoGcsFileIdMappingCollectionMigration;
import io.harness.migrations.all.DropOldCollectionMigration;
import io.harness.migrations.all.DropStringCollectionMigration;
import io.harness.migrations.all.DropUniqueIndexOnImportedTemplate;
import io.harness.migrations.all.DropUniqueIndexOnTemplateGallery;
import io.harness.migrations.all.DropYamlGitSyncCollectionMigration;
import io.harness.migrations.all.DuplicateGlobalAccountMigration;
import io.harness.migrations.all.EnableIteratorsForLdapSync;
import io.harness.migrations.all.EntityNameValidationMigration_All_00;
import io.harness.migrations.all.EntityNameValidationMigration_All_01;
import io.harness.migrations.all.EntityNameValidationMigration_All_02;
import io.harness.migrations.all.EntityNameValidationMigration_All_03;
import io.harness.migrations.all.EntityNameValidationMigration_All_04;
import io.harness.migrations.all.FixCVDashboardStatusMigration;
import io.harness.migrations.all.GcpServiceAccountMigration;
import io.harness.migrations.all.GcsArtifactProjectIdMigration;
import io.harness.migrations.all.GitCommitStatusMigration;
import io.harness.migrations.all.GitSyncToAllAccounts;
import io.harness.migrations.all.HelmReleaseNamePrefixMigration;
import io.harness.migrations.all.HelmReleaseNameSuffixMigration;
import io.harness.migrations.all.HelmValuesYamlToManifestFileMigration;
import io.harness.migrations.all.ImportedTemplateGalleryMigration;
import io.harness.migrations.all.InfraProvisionerFilteringTypeMigration;
import io.harness.migrations.all.InitInfraProvisionerCounters;
import io.harness.migrations.all.InitPipelineCounters;
import io.harness.migrations.all.InitServiceCounters;
import io.harness.migrations.all.InitUserCounters;
import io.harness.migrations.all.InitWorkflowCounters;
import io.harness.migrations.all.InstanceComputerProviderNameFixMigration;
import io.harness.migrations.all.K8sV2ServiceInfraReleaseNameMigration;
import io.harness.migrations.all.LdapSettingsMigration;
import io.harness.migrations.all.LearningEngineTaskGroupNameMigration;
import io.harness.migrations.all.LicenseDataMigration;
import io.harness.migrations.all.LimitCounterAccountIdMigration;
import io.harness.migrations.all.LogAnalysisDeprecatedRecordMigration;
import io.harness.migrations.all.LogAnalysisExperimentalRecordsMigration;
import io.harness.migrations.all.LoginRateLimitMigration;
import io.harness.migrations.all.ManageSecretsMigration;
import io.harness.migrations.all.MarketoLeadDataMigration;
import io.harness.migrations.all.MetricAnalysisRecordGroupNameMigration;
import io.harness.migrations.all.MetricDataRecordGroupNameMigration;
import io.harness.migrations.all.MetricMLAnalysisRecordGroupNameMigration;
import io.harness.migrations.all.MigrateCVMetadataApplicationId;
import io.harness.migrations.all.MigrateCloudwatchCVTemplates;
import io.harness.migrations.all.MigrateLogFeedbackRecordsToGoogle;
import io.harness.migrations.all.NewRelicMetricAnalysisRecordsMigration;
import io.harness.migrations.all.NewRelicMetricDataBaselineMigration;
import io.harness.migrations.all.NewRelicMetricDataGroupNameMigration;
import io.harness.migrations.all.NewRelicMetricDataRecordsMigration;
import io.harness.migrations.all.NexusDockerArtifactStreamMigration;
import io.harness.migrations.all.NonWorkflowCVConfigurationMigration;
import io.harness.migrations.all.OAuthAllowedProvidersListMigration;
import io.harness.migrations.all.OauthEnabledFieldMigration;
import io.harness.migrations.all.OverrideDefaultLimits;
import io.harness.migrations.all.PcfServiceDeploymentMigration;
import io.harness.migrations.all.PcfServiceSpecificationToManifestFileMigration;
import io.harness.migrations.all.PerpetualTaskIteratorMigration;
import io.harness.migrations.all.PerpetualTaskMigration;
import io.harness.migrations.all.PipelineWorkflowExecutionActionMigration;
import io.harness.migrations.all.PipelineWorkflowExecutionActionQlMigration;
import io.harness.migrations.all.PreferenceUserIdRemoveDollarSignMigration;
import io.harness.migrations.all.PrometheusCVMigration;
import io.harness.migrations.all.QpsGraphQLMigration;
import io.harness.migrations.all.RemoveDupInstanceStats;
import io.harness.migrations.all.RemoveResizeFromStatefulSetWorkflows;
import io.harness.migrations.all.RemoveServiceVariablesFromActivity;
import io.harness.migrations.all.RemoveUnusedLogDataRecordMigration;
import io.harness.migrations.all.ResourceLookupMigration;
import io.harness.migrations.all.ScheduleSegmentPublishJob;
import io.harness.migrations.all.SecretManagerPermissionMigration;
import io.harness.migrations.all.SecretTextNameKeyWordsMigration;
import io.harness.migrations.all.ServerlessInstanceChangeCollectionNameMigration;
import io.harness.migrations.all.ServiceAddArtifactStreamIdsMigration;
import io.harness.migrations.all.ServiceHelmValuesToManifestFileMigration;
import io.harness.migrations.all.ServiceNameMigrationIfEmpty;
import io.harness.migrations.all.SetIsDeletedFlagForInstances;
import io.harness.migrations.all.SetNamespaceInContainerInstanceInfo;
import io.harness.migrations.all.SetNamespaceToKubernetesInstanceInfo;
import io.harness.migrations.all.SetRollbackFlagToWorkflows;
import io.harness.migrations.all.SettingAttributesCategoryMigration;
import io.harness.migrations.all.SshAndWinRmAccountPermissionMigration;
import io.harness.migrations.all.StackdriverMetricServiceGuardJsonMigration;
import io.harness.migrations.all.StackdriverServiceGuardMetricsGroupingMigration;
import io.harness.migrations.all.SweepingPhaseMigration;
import io.harness.migrations.all.SweepingStateMigration;
import io.harness.migrations.all.SystemTemplateGalleryMigration;
import io.harness.migrations.all.TimeSeriesAnalysisRecordsMigration;
import io.harness.migrations.all.TimeSeriesMLAnalysisCompressionSaveMigration;
import io.harness.migrations.all.TimeSeriesMLAnalysisDeleteUncompressedMigration;
import io.harness.migrations.all.TimeSeriesMLScoresMigration;
import io.harness.migrations.all.TrimURLsForAPMVerificationSettings;
import io.harness.migrations.all.UnregisteredUserNameMigration;
import io.harness.migrations.all.UpdateBitBucketTriggers;
import io.harness.migrations.all.UpdateCVTaskIterationMigration;
import io.harness.migrations.all.UpdateGitSyncErrorMigration;
import io.harness.migrations.all.UpdatePipelineParallelIndexes;
import io.harness.migrations.all.UsageRestrictionsMigration;
import io.harness.migrations.all.UserPermissionReadMigration;
import io.harness.migrations.all.VaultAppRoleRenewalMigration;
import io.harness.migrations.all.VaultConfigRenewalIntervalMigration;
import io.harness.migrations.all.VerificationMetricJobMigration;
import io.harness.migrations.all.YamlGitConfigAppMigration;
import io.harness.migrations.all.YamlGitConfigMigration;
import io.harness.migrations.all.YamlGitConfigRefactoringMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
@UtilityClass
public class MigrationList {
  /**
   * Add your migrations to the end of the list with the next sequence number. After it has been in production for a few
   * releases it can be deleted, but keep at least one item in this list with the latest sequence number. You can use
   * BaseMigration.class as a placeholder for any removed class.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(130, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(131, LogAnalysisExperimentalRecordsMigration.class))
        .add(Pair.of(132, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(133, NewRelicMetricAnalysisRecordsMigration.class))
        .add(Pair.of(134, NewRelicMetricDataRecordsMigration.class))
        .add(Pair.of(135, TimeSeriesAnalysisRecordsMigration.class))
        .add(Pair.of(136, TimeSeriesMLScoresMigration.class))
        .add(Pair.of(137, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(138, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(139, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(140, GitSyncToAllAccounts.class))
        .add(Pair.of(141, RemoveResizeFromStatefulSetWorkflows.class))
        .add(Pair.of(142, MetricDataRecordGroupNameMigration.class))
        .add(Pair.of(143, MetricAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(144, MetricMLAnalysisRecordGroupNameMigration.class))
        .add(Pair.of(145, LearningEngineTaskGroupNameMigration.class))
        .add(Pair.of(146, AddValidUntilToAlert.class))
        .add(Pair.of(147, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(148, EntityNameValidationMigration_All_00.class))
        .add(Pair.of(149, NewRelicMetricDataGroupNameMigration.class))
        .add(Pair.of(150, AddValidUntilToDelegateTask.class))
        .add(Pair.of(151, AddValidUntilToActivity.class))
        .add(Pair.of(152, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(153, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(154, EntityNameValidationMigration_All_01.class))
        .add(Pair.of(155, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(156, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(157, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(158, EntityNameValidationMigration_All_02.class))
        .add(Pair.of(159, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(160, EntityNameValidationMigration_All_03.class))
        .add(Pair.of(161, DropStringCollectionMigration.class))
        .add(Pair.of(162, DropDelegateScopeCollectionMigration.class))
        .add(Pair.of(163, PreferenceUserIdRemoveDollarSignMigration.class))
        .add(Pair.of(164, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(165, DropYamlGitSyncCollectionMigration.class))
        .add(Pair.of(166, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(167, AddReplCtrlNameToKubeSetupProperties.class))
        .add(Pair.of(168, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(169, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(170, EntityNameValidationMigration_All_04.class))
        .add(Pair.of(171, DropOldCollectionMigration.class))
        .add(Pair.of(172, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(173, SystemTemplateGalleryMigration.class))
        .add(Pair.of(174, AppKeywordsMigration.class))
        .add(Pair.of(175, DeleteOldThirdPartyApiCallsMigration.class))
        .add(Pair.of(176, UnregisteredUserNameMigration.class))
        .add(Pair.of(177, DeleteStaleYamlChangeSetsMigration.class))
        .add(Pair.of(178, AuthTokenTtlMigration.class))
        .add(Pair.of(179, DeleteStaleSecretUsageLogs.class))
        .add(Pair.of(180, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(181, HelmReleaseNamePrefixMigration.class))
        .add(Pair.of(182, CleanupOrphanInstances.class))
        .add(Pair.of(183, InfraProvisionerFilteringTypeMigration.class))
        .add(Pair.of(184, CleanUpDatadogCallLogMigration.class))
        .add(Pair.of(185, SweepingPhaseMigration.class))
        .add(Pair.of(186, SecretTextNameKeyWordsMigration.class))
        .add(Pair.of(187, GcsArtifactProjectIdMigration.class))
        .add(Pair.of(188, FixCVDashboardStatusMigration.class))
        .add(Pair.of(189, RemoveServiceVariablesFromActivity.class))
        .add(Pair.of(190, DeleteLETaskDelCronMigration.class))
        .add(Pair.of(191, HelmReleaseNameSuffixMigration.class))
        .add(Pair.of(192, SetIsDeletedFlagForInstances.class))
        .add(Pair.of(193, UsageRestrictionsMigration.class))
        .add(Pair.of(194, AddInstanceStatsCollectionJobToAllAccounts.class))
        .add(Pair.of(195, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(196, RemoveDupInstanceStats.class))
        .add(Pair.of(197, YamlGitConfigRefactoringMigration.class))
        .add(Pair.of(198, YamlGitConfigMigration.class))
        .add(Pair.of(199, YamlGitConfigAppMigration.class))
        .add(Pair.of(200, UpdateGitSyncErrorMigration.class))
        .add(Pair.of(201, RemoveDupInstanceStats.class))
        .add(Pair.of(202, RemoveDupInstanceStats.class))
        .add(Pair.of(203, UpdateGitSyncErrorMigration.class)) // This is intentional.
        .add(Pair.of(204, InitializeAppCounters.class))
        .add(Pair.of(205, DanglingAppEnvReferenceRemovalMigration.class))
        .add(Pair.of(206, TerraformProvisionerBranchMigration.class))
        .add(Pair.of(207, SetNamespaceToKubernetesInstanceInfo.class))
        .add(Pair.of(208, DeleteNewRelicMetricNameCronMigration.class))
        .add(Pair.of(209, TimeSeriesMLAnalysisCompressionSaveMigration.class))
        .add(Pair.of(210, TimeSeriesMLAnalysisDeleteUncompressedMigration.class))
        .add(Pair.of(211, LicenseDataMigration.class))
        .add(Pair.of(212, DuplicateGlobalAccountMigration.class))
        .add(Pair.of(213, OverrideDefaultLimits.class))
        .add(Pair.of(214, MarketoLeadDataMigration.class))
        .add(Pair.of(215, DanglingUserInviteCleanupMigration.class))
        .add(Pair.of(216, MarketoLeadDataMigration.class))
        .add(Pair.of(217, LdapSettingsMigration.class))
        .add(Pair.of(218, InitUserCounters.class))
        .add(Pair.of(219, InitPipelineCounters.class))
        .add(Pair.of(220, AppDTemplateMigration.class))
        .add(Pair.of(221, AddLimitVicinityCheckJobToAllAccounts.class))
        .add(Pair.of(222, HelmValuesYamlToManifestFileMigration.class))
        .add(Pair.of(223, InitWorkflowCounters.class))
        .add(Pair.of(224, CreateDefaultAlertNotificationRule.class))
        .add(Pair.of(225, InitServiceCounters.class))
        .add(Pair.of(226, InitInfraProvisionerCounters.class))
        .add(Pair.of(227, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(228, NewRelicMetricDataBaselineMigration.class))
        .add(Pair.of(229, AddAccountIdToAppEntities.class))
        .add(Pair.of(230, MigrateCVMetadataApplicationId.class))
        .add(Pair.of(231, DropMongoGcsFileIdMappingCollectionMigration.class))
        .add(Pair.of(232, AddTemplateMgmtPermissionToAdminUserGroup.class))
        .add(Pair.of(233, CVCollectionCronFrequencyMigration.class))
        .add(Pair.of(234, VerificationMetricJobMigration.class))
        .add(Pair.of(235, NexusDockerArtifactStreamMigration.class))
        .add(Pair.of(236, LogAnalysisDeprecatedRecordMigration.class))
        .add(Pair.of(237, K8sV2ServiceInfraReleaseNameMigration.class))
        .add(Pair.of(238, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(239, ServiceNameMigrationIfEmpty.class))
        .add(Pair.of(240, DeleteCVAlertsMigration.class))
        .add(Pair.of(241, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(242, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(243, ServiceHelmValuesToManifestFileMigration.class))
        .add(Pair.of(244, SetNamespaceInContainerInstanceInfo.class))
        .add(Pair.of(245, UpdateBitBucketTriggers.class))
        .add(Pair.of(246, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(247, UserPermissionReadMigration.class))
        .add(Pair.of(248, ServiceAddArtifactStreamIdsMigration.class))
        .add(Pair.of(249, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(250, AddAccountIdToArtifactsMigration.class))
        .add(Pair.of(251, AddAccountIdToArtifactStreamsMigration.class))
        .add(Pair.of(252, RemoveUnusedLogDataRecordMigration.class))
        .add(Pair.of(253, DatadogCVServiceConfigurationMigration.class))
        .add(Pair.of(254, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(255, OAuthAllowedProvidersListMigration.class))
        .add(Pair.of(256, MigrateLogFeedbackRecordsToGoogle.class))
        .add(Pair.of(257, NonWorkflowCVConfigurationMigration.class))
        .add(Pair.of(258, AddWhitelistedDomainsToAccountMigration.class))
        .add(Pair.of(259, OauthEnabledFieldMigration.class))
        .add(Pair.of(260, AddLoginSettingsToAccountMigration.class))
        .add(Pair.of(261, AuditRecordMigration.class))
        .add(Pair.of(262, ResourceLookupMigration.class))
        .add(Pair.of(263, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(264, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(265, AuditViewerPermissionMigration.class))
        .add(Pair.of(266, AwsAmiAsgDesiredInstancesMigration.class))
        .add(Pair.of(267, LimitCounterAccountIdMigration.class))
        .add(Pair.of(268, UpdatePipelineParallelIndexes.class))
        .add(Pair.of(269, AddDuplicateFieldToCVFeedbacks.class))
        .add(Pair.of(270, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(271, MigrateCloudwatchCVTemplates.class))
        .add(Pair.of(272, CloudWatchCVMigration.class))
        .add(Pair.of(273, AddTagManagementPermissionToAdminUserGroup.class))
        .add(Pair.of(274, PrometheusCVMigration.class))
        .add(Pair.of(275, ScheduleSegmentPublishJob.class))
        .add(Pair.of(276, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(277, SweepingStateMigration.class))
        .add(Pair.of(278, DatadogCustomMetricMigration.class))
        .add(Pair.of(279, SettingAttributesCategoryMigration.class))
        .add(Pair.of(280, ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes.class))
        .add(Pair.of(281, PcfServiceSpecificationToManifestFileMigration.class))
        .add(Pair.of(282, PcfServiceDeploymentMigration.class))
        .add(Pair.of(283, StackdriverServiceGuardMetricsGroupingMigration.class))
        .add(Pair.of(284, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(285, LoginRateLimitMigration.class))
        .add(Pair.of(286, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(287, InstanceComputerProviderNameFixMigration.class))
        .add(Pair.of(288, DeleteServiceGuardAlertMigration.class))
        .add(Pair.of(289, HelmConnectorPathMigration.class))
        .add(Pair.of(290, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(291, CleanupOrphanInstances.class))
        .add(Pair.of(292, CleanupSyncStatusForDeletedEntities.class))
        .add(Pair.of(293, DeleteCVCronMigration.class))
        .add(Pair.of(294, AmiDeploymentTypeMigration.class))
        .add(Pair.of(295, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(296, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(297, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(298, GitCommitStatusMigration.class))
        .add(Pair.of(299, DataDogLogCvConfigMigration.class))
        .add(Pair.of(300, HostConnectionTypeMigration.class))
        .add(Pair.of(301, AddAccountIdToAppEntities.class))
        .add(Pair.of(302, DropExistingIndexForGitFileActivityMigration.class))
        .add(Pair.of(303, DeleteCVCronMigration.class))
        .add(Pair.of(304, DeleteGitActivityWithoutProcCommitIdMigration.class))
        .add(Pair.of(305, ImportedTemplateGalleryMigration.class))
        .add(Pair.of(306, DropUniqueIndexOnTemplateGallery.class))
        .add(Pair.of(307, AddHarnessCommandLibraryToAccount.class))
        .add(Pair.of(308, AddScopedToAccountAttributeToEncryptedData.class))
        .add(Pair.of(309, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(310, DropAppIdIndexOnCommandLogs.class))
        .add(Pair.of(311, StackdriverMetricServiceGuardJsonMigration.class))
        .add(Pair.of(312, DropUniqueIndexOnImportedTemplate.class))
        .add(Pair.of(313, PipelineWorkflowExecutionActionMigration.class))
        .add(Pair.of(314, VaultConfigRenewalIntervalMigration.class))
        .add(Pair.of(315, TrimURLsForAPMVerificationSettings.class))
        .add(Pair.of(316, PipelineWorkflowExecutionActionQlMigration.class))
        .add(Pair.of(317, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(318, AddCEPermissionToAllUserGroups.class))
        .add(Pair.of(319, AddDisabledFieldMigration.class))
        .add(Pair.of(320, ArtifactStreamIteratorRequiredMigration.class))
        .add(Pair.of(321, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(322, UpdateCVTaskIterationMigration.class))
        .add(Pair.of(323, VaultAppRoleRenewalMigration.class))
        .add(Pair.of(324, ServerlessInstanceChangeCollectionNameMigration.class))
        .add(Pair.of(325, ManageCloudProvidersAndConnectorsPermissionMigration.class))
        .add(Pair.of(326, SecretManagerPermissionMigration.class))
        .add(Pair.of(327, ManageSecretsMigration.class))
        .add(Pair.of(328, ManageIPWhitelistPermissionMigration.class))
        .add(Pair.of(329, AlertNotificationAccountPermissionMigration.class))
        .add(Pair.of(330, ManageAuthenticationSettingsPermissionMigration.class))
        .add(Pair.of(331, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(332, AddHarnessGroupAccessFlagToAccount.class))
        .add(Pair.of(333, ManageDelegatePermissionMigration.class))
        .add(Pair.of(334, ManageDelegateProfilesPermissionMigration.class))
        .add(Pair.of(335, AddValidUntilToDelegate.class))
        .add(Pair.of(336, ManageApiKeyPermissionMigration.class))
        .add(Pair.of(337, RemoveRedundantAccountPermissions.class))
        .add(Pair.of(338, ManageTagsMigration.class))
        .add(Pair.of(339, ManageApplicationsPermissionMigration.class))
        .add(Pair.of(340, ManageDeploymentFreezePermissionMigration.class))
        .add(Pair.of(341, ManagePipelineGovernancePermissionMigration.class))
        .add(Pair.of(342, RemoveDeprecatedTagManagementPermission.class))
        .add(Pair.of(343, PerpetualTaskMigration.class))
        .add(Pair.of(344, RemoveDeprecatedApplicationsCreatePermission.class))
        .add(Pair.of(345, ManageConfigAsCodePermissionMigration.class))
        .add(Pair.of(346, AddManageTagsPermission.class))
        .add(Pair.of(347, io.harness.migrations.BaseMigration.class))
        .add(Pair.of(348, PerpetualTaskIteratorMigration.class))
        .add(Pair.of(349, ManageCustomDashboardPermissionMigration.class))
        .add(Pair.of(350, CreateCustomDashboardPermissionMigration.class))
        .add(Pair.of(351, AddRestrictionsToSecretManagerConfig.class))
        .add(Pair.of(352, AwsConfigEc2IamRoleMigration.class))
        .add(Pair.of(353, ChangeApiKeyHashFunction.class))
        .add(Pair.of(354, BaseMigration.class))
        .add(Pair.of(355, BaseMigration.class))
        .add(Pair.of(356, CECloudAccountMigration.class))
        .add(Pair.of(357, SshAndWinRmAccountPermissionMigration.class))
        .add(Pair.of(358, BaseMigration.class))
        .add(Pair.of(359, BaseMigration.class))
        .add(Pair.of(360, AddEnableIteratorsToTriggers.class))
        .add(Pair.of(361, AddNgFieldToDelegateGroupMigration.class))
        .add(Pair.of(362, BaseMigration.class))
        .add(Pair.of(363, QpsGraphQLMigration.class))
        .add(Pair.of(364, BaseMigration.class))
        .add(Pair.of(365, BaseMigration.class))
        .add(Pair.of(366, EnableIteratorsForLdapSync.class))
        .add(Pair.of(367, GcpServiceAccountMigration.class))
        .add(Pair.of(368, DisableWinrmVariablesFFMigration.class))
        .add(Pair.of(369, DisableAddingServiceVarsToEcsSpecFFMigration.class))
        .add(Pair.of(370, ManageAccountDefaultsPermissionMigration.class))
        .add(Pair.of(371, BaseMigration.class))
        .add(Pair.of(372, BaseMigration.class))
        .build();
  }
}
