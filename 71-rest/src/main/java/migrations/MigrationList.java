
package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddAccountIdToAppEntities;
import migrations.all.AddAccountIdToArtifactStreamsMigration;
import migrations.all.AddAccountIdToArtifactsMigration;
import migrations.all.AddDuplicateFieldToCVFeedbacks;
import migrations.all.AddInstanceStatsCollectionJobToAllAccounts;
import migrations.all.AddLimitVicinityCheckJobToAllAccounts;
import migrations.all.AddLoginSettingsToAccountMigration;
import migrations.all.AddReplCtrlNameToKubeSetupProperties;
import migrations.all.AddTagManagementPermissionToAdminUserGroup;
import migrations.all.AddTemplateMgmtPermissionToAdminUserGroup;
import migrations.all.AddValidUntilToActivity;
import migrations.all.AddValidUntilToAlert;
import migrations.all.AddValidUntilToDelegateTask;
import migrations.all.AddWhitelistedDomainsToAccountMigration;
import migrations.all.AppDTemplateMigration;
import migrations.all.AppKeywordsMigration;
import migrations.all.AuditRecordMigration;
import migrations.all.AuditViewerPermissionMigration;
import migrations.all.AuthTokenTtlMigration;
import migrations.all.AwsAmiAsgDesiredInstancesMigration;
import migrations.all.CVCollectionCronFrequencyMigration;
import migrations.all.CleanUpDatadogCallLogMigration;
import migrations.all.CleanupOrphanInstances;
import migrations.all.CloudWatchCVMigration;
import migrations.all.ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes;
import migrations.all.CreateDefaultAlertNotificationRule;
import migrations.all.DanglingAppEnvReferenceRemovalMigration;
import migrations.all.DanglingUserInviteCleanupMigration;
import migrations.all.DatadogCVServiceConfigurationMigration;
import migrations.all.DatadogCustomMetricMigration;
import migrations.all.DeleteCVAlertsMigration;
import migrations.all.DeleteLETaskDelCronMigration;
import migrations.all.DeleteNewRelicMetricNameCronMigration;
import migrations.all.DeleteOldThirdPartyApiCallsMigration;
import migrations.all.DeleteStaleSecretUsageLogs;
import migrations.all.DeleteStaleYamlChangeSetsMigration;
import migrations.all.DropDelegateScopeCollectionMigration;
import migrations.all.DropMongoGcsFileIdMappingCollectionMigration;
import migrations.all.DropOldCollectionMigration;
import migrations.all.DropStringCollectionMigration;
import migrations.all.DropYamlGitSyncCollectionMigration;
import migrations.all.DuplicateGlobalAccountMigration;
import migrations.all.EntityNameValidationMigration_All_00;
import migrations.all.EntityNameValidationMigration_All_01;
import migrations.all.EntityNameValidationMigration_All_02;
import migrations.all.EntityNameValidationMigration_All_03;
import migrations.all.EntityNameValidationMigration_All_04;
import migrations.all.FixCVDashboardStatusMigration;
import migrations.all.GcsArtifactProjectIdMigration;
import migrations.all.GitSyncToAllAccounts;
import migrations.all.HelmReleaseNamePrefixMigration;
import migrations.all.HelmReleaseNameSuffixMigration;
import migrations.all.HelmValuesYamlToManifestFileMigration;
import migrations.all.InfraProvisionerFilteringTypeMigration;
import migrations.all.InitInfraProvisionerCounters;
import migrations.all.InitPipelineCounters;
import migrations.all.InitServiceCounters;
import migrations.all.InitUserCounters;
import migrations.all.InitWorkflowCounters;
import migrations.all.InstanceComputerProviderNameFixMigration;
import migrations.all.K8sV2ServiceInfraReleaseNameMigration;
import migrations.all.LdapSettingsMigration;
import migrations.all.LearningEngineTaskGroupNameMigration;
import migrations.all.LicenseDataMigration;
import migrations.all.LimitCounterAccountIdMigration;
import migrations.all.LogAnalysisDeprecatedRecordMigration;
import migrations.all.LogAnalysisExperimentalRecordsMigration;
import migrations.all.LoginRateLimitMigration;
import migrations.all.MarketoLeadDataMigration;
import migrations.all.MetricAnalysisRecordGroupNameMigration;
import migrations.all.MetricDataRecordGroupNameMigration;
import migrations.all.MetricMLAnalysisRecordGroupNameMigration;
import migrations.all.MigrateCVMetadataApplicationId;
import migrations.all.MigrateCloudwatchCVTemplates;
import migrations.all.MigrateLogFeedbackRecordsToGoogle;
import migrations.all.NewRelicMetricAnalysisRecordsMigration;
import migrations.all.NewRelicMetricDataBaselineMigration;
import migrations.all.NewRelicMetricDataGroupNameMigration;
import migrations.all.NewRelicMetricDataRecordsMigration;
import migrations.all.NexusDockerArtifactStreamMigration;
import migrations.all.NonWorkflowCVConfigurationMigration;
import migrations.all.OAuthAllowedProvidersListMigration;
import migrations.all.OauthEnabledFieldMigration;
import migrations.all.OverrideDefaultLimits;
import migrations.all.PcfServiceDeploymentMigration;
import migrations.all.PcfServiceSpecificationToManifestFileMigration;
import migrations.all.PreferenceUserIdRemoveDollarSignMigration;
import migrations.all.PrometheusCVMigration;
import migrations.all.RemoveDupInstanceStats;
import migrations.all.RemoveResizeFromStatefulSetWorkflows;
import migrations.all.RemoveServiceVariablesFromActivity;
import migrations.all.RemoveUnusedLogDataRecordMigration;
import migrations.all.ResourceLookupMigration;
import migrations.all.ScheduleSegmentPublishJob;
import migrations.all.SecretManagerConfigMigration;
import migrations.all.SecretTextNameKeyWordsMigration;
import migrations.all.ServiceAddArtifactStreamIdsMigration;
import migrations.all.ServiceHelmValuesToManifestFileMigration;
import migrations.all.ServiceNameMigrationIfEmpty;
import migrations.all.SetIsDeletedFlagForInstances;
import migrations.all.SetNamespaceInContainerInstanceInfo;
import migrations.all.SetNamespaceToKubernetesInstanceInfo;
import migrations.all.SetRollbackFlagToWorkflows;
import migrations.all.SettingAttributesCategoryMigration;
import migrations.all.StackdriverServiceGuardMetricsGroupingMigration;
import migrations.all.SweepingPhaseMigration;
import migrations.all.SweepingStateMigration;
import migrations.all.SystemTemplateGalleryMigration;
import migrations.all.TimeSeriesAnalysisRecordsMigration;
import migrations.all.TimeSeriesMLAnalysisCompressionSaveMigration;
import migrations.all.TimeSeriesMLAnalysisDeleteUncompressedMigration;
import migrations.all.TimeSeriesMLScoresMigration;
import migrations.all.UnregisteredUserNameMigration;
import migrations.all.UpdateBitBucketTriggers;
import migrations.all.UpdateGitSyncErrorMigration;
import migrations.all.UpdatePipelineParallelIndexes;
import migrations.all.UsageRestrictionsMigration;
import migrations.all.UserPermissionReadMigration;
import migrations.all.VerificationMetricJobMigration;
import migrations.all.YamlGitConfigAppMigration;
import migrations.all.YamlGitConfigMigration;
import migrations.all.YamlGitConfigRefactoringMigration;
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
        .add(Pair.of(130, BaseMigration.class))
        .add(Pair.of(131, LogAnalysisExperimentalRecordsMigration.class))
        .add(Pair.of(132, BaseMigration.class))
        .add(Pair.of(133, NewRelicMetricAnalysisRecordsMigration.class))
        .add(Pair.of(134, NewRelicMetricDataRecordsMigration.class))
        .add(Pair.of(135, TimeSeriesAnalysisRecordsMigration.class))
        .add(Pair.of(136, TimeSeriesMLScoresMigration.class))
        .add(Pair.of(137, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(138, BaseMigration.class))
        .add(Pair.of(139, BaseMigration.class))
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
        .add(Pair.of(164, BaseMigration.class))
        .add(Pair.of(165, DropYamlGitSyncCollectionMigration.class))
        .add(Pair.of(166, BaseMigration.class))
        .add(Pair.of(167, AddReplCtrlNameToKubeSetupProperties.class))
        .add(Pair.of(168, BaseMigration.class))
        .add(Pair.of(169, BaseMigration.class))
        .add(Pair.of(170, EntityNameValidationMigration_All_04.class))
        .add(Pair.of(171, DropOldCollectionMigration.class))
        .add(Pair.of(172, BaseMigration.class))
        .add(Pair.of(173, SystemTemplateGalleryMigration.class))
        .add(Pair.of(174, AppKeywordsMigration.class))
        .add(Pair.of(175, DeleteOldThirdPartyApiCallsMigration.class))
        .add(Pair.of(176, UnregisteredUserNameMigration.class))
        .add(Pair.of(177, DeleteStaleYamlChangeSetsMigration.class))
        .add(Pair.of(178, AuthTokenTtlMigration.class))
        .add(Pair.of(179, DeleteStaleSecretUsageLogs.class))
        .add(Pair.of(180, BaseMigration.class))
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
        .add(Pair.of(195, BaseMigration.class))
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
        .add(Pair.of(227, BaseMigration.class))
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
        .add(Pair.of(238, BaseMigration.class))
        .add(Pair.of(239, ServiceNameMigrationIfEmpty.class))
        .add(Pair.of(240, DeleteCVAlertsMigration.class))
        .add(Pair.of(241, BaseMigration.class))
        .add(Pair.of(242, BaseMigration.class))
        .add(Pair.of(243, ServiceHelmValuesToManifestFileMigration.class))
        .add(Pair.of(244, SetNamespaceInContainerInstanceInfo.class))
        .add(Pair.of(245, UpdateBitBucketTriggers.class))
        .add(Pair.of(246, BaseMigration.class))
        .add(Pair.of(247, UserPermissionReadMigration.class))
        .add(Pair.of(248, ServiceAddArtifactStreamIdsMigration.class))
        .add(Pair.of(249, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(250, AddAccountIdToArtifactsMigration.class))
        .add(Pair.of(251, AddAccountIdToArtifactStreamsMigration.class))
        .add(Pair.of(252, RemoveUnusedLogDataRecordMigration.class))
        .add(Pair.of(253, DatadogCVServiceConfigurationMigration.class))
        .add(Pair.of(254, SecretManagerConfigMigration.class))
        .add(Pair.of(255, OAuthAllowedProvidersListMigration.class))
        .add(Pair.of(256, MigrateLogFeedbackRecordsToGoogle.class))
        .add(Pair.of(257, NonWorkflowCVConfigurationMigration.class))
        .add(Pair.of(258, AddWhitelistedDomainsToAccountMigration.class))
        .add(Pair.of(259, OauthEnabledFieldMigration.class))
        .add(Pair.of(260, AddLoginSettingsToAccountMigration.class))
        .add(Pair.of(261, AuditRecordMigration.class))
        .add(Pair.of(262, ResourceLookupMigration.class))
        .add(Pair.of(263, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(264, BaseMigration.class))
        .add(Pair.of(265, AuditViewerPermissionMigration.class))
        .add(Pair.of(266, AwsAmiAsgDesiredInstancesMigration.class))
        .add(Pair.of(267, LimitCounterAccountIdMigration.class))
        .add(Pair.of(268, UpdatePipelineParallelIndexes.class))
        .add(Pair.of(269, AddDuplicateFieldToCVFeedbacks.class))
        .add(Pair.of(270, BaseMigration.class))
        .add(Pair.of(271, MigrateCloudwatchCVTemplates.class))
        .add(Pair.of(272, CloudWatchCVMigration.class))
        .add(Pair.of(273, AddTagManagementPermissionToAdminUserGroup.class))
        .add(Pair.of(274, PrometheusCVMigration.class))
        .add(Pair.of(275, ScheduleSegmentPublishJob.class))
        .add(Pair.of(276, BaseMigration.class))
        .add(Pair.of(277, SweepingStateMigration.class))
        .add(Pair.of(278, DatadogCustomMetricMigration.class))
        .add(Pair.of(279, SettingAttributesCategoryMigration.class))
        .add(Pair.of(280, ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes.class))
        .add(Pair.of(281, PcfServiceSpecificationToManifestFileMigration.class))
        .add(Pair.of(282, PcfServiceDeploymentMigration.class))
        .add(Pair.of(283, StackdriverServiceGuardMetricsGroupingMigration.class))
        .add(Pair.of(284, BaseMigration.class))
        .add(Pair.of(285, LoginRateLimitMigration.class))
        .add(Pair.of(286, BaseMigration.class))
        .add(Pair.of(287, InstanceComputerProviderNameFixMigration.class))
        .build();
  }
}