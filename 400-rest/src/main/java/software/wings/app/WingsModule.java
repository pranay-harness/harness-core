package software.wings.app;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationStepsModule;
import io.harness.OrchestrationVisualizationModule;
import io.harness.SecretManagementCoreModule;
import io.harness.annotations.retry.MethodExecutionHelper;
import io.harness.annotations.retry.RetryOnException;
import io.harness.annotations.retry.RetryOnExceptionInterceptor;
import io.harness.artifacts.gcr.service.GcrApiService;
import io.harness.artifacts.gcr.service.GcrApiServiceImpl;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.ccm.anomaly.service.impl.AnomalyServiceImpl;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.billing.GcpBillingService;
import io.harness.ccm.billing.GcpBillingServiceImpl;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.bigquery.BigQueryServiceImpl;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingService;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.BudgetServiceImpl;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.ClusterRecordServiceImpl;
import io.harness.ccm.communication.CECommunicationsService;
import io.harness.ccm.communication.CECommunicationsServiceImpl;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.CESlackWebhookServiceImpl;
import io.harness.ccm.config.CCMSettingService;
import io.harness.ccm.config.CCMSettingServiceImpl;
import io.harness.ccm.health.HealthStatusService;
import io.harness.ccm.health.HealthStatusServiceImpl;
import io.harness.ccm.setup.CESetupServiceModule;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEReportTemplateBuilderService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.ccm.views.service.impl.CEReportScheduleServiceImpl;
import io.harness.ccm.views.service.impl.CEReportTemplateBuilderServiceImpl;
import io.harness.ccm.views.service.impl.CEViewServiceImpl;
import io.harness.ccm.views.service.impl.ViewCustomFieldServiceImpl;
import io.harness.ccm.views.service.impl.ViewsBillingServiceImpl;
import io.harness.cf.ApiException;
import io.harness.cf.CfClientConfig;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.api.CfClientException;
import io.harness.cf.client.api.exception.CFException;
import io.harness.config.PipelineConfig;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.cvng.CVNextGenCommonsServiceModule;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.client.CVNGServiceImpl;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskServiceImpl;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.cvng.state.CVNGVerificationTaskServiceImpl;
import io.harness.dashboard.DashboardSettingsService;
import io.harness.dashboard.DashboardSettingsServiceImpl;
import io.harness.datahandler.services.AdminAccountService;
import io.harness.datahandler.services.AdminAccountServiceImpl;
import io.harness.datahandler.services.AdminFeatureFlagService;
import io.harness.datahandler.services.AdminFeatureFlagServiceImpl;
import io.harness.datahandler.services.AdminUserService;
import io.harness.datahandler.services.AdminUserServiceImpl;
import io.harness.datahandler.utils.AccountSummaryHelper;
import io.harness.datahandler.utils.AccountSummaryHelperImpl;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.git.NGGitServiceImpl;
import io.harness.encryptors.CustomEncryptor;
import io.harness.encryptors.Encryptors;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.clients.AwsKmsEncryptor;
import io.harness.encryptors.clients.GcpKmsEncryptor;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.encryptors.managerproxy.ManagerCustomEncryptor;
import io.harness.encryptors.managerproxy.ManagerKmsEncryptor;
import io.harness.encryptors.managerproxy.ManagerVaultEncryptor;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.event.handler.impl.segment.SegmentGroupEventJobService;
import io.harness.event.handler.impl.segment.SegmentGroupEventJobServiceImpl;
import io.harness.event.reconciliation.service.DeploymentReconService;
import io.harness.event.reconciliation.service.DeploymentReconServiceImpl;
import io.harness.event.timeseries.processor.instanceeventprocessor.instancereconservice.IInstanceReconService;
import io.harness.event.timeseries.processor.instanceeventprocessor.instancereconservice.InstanceReconServiceImpl;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ff.FeatureFlagModule;
import io.harness.git.GitClientV2;
import io.harness.git.GitClientV2Impl;
import io.harness.govern.ServersModule;
import io.harness.governance.pipeline.service.GovernanceStatusEvaluator;
import io.harness.governance.pipeline.service.PipelineGovernanceService;
import io.harness.governance.pipeline.service.PipelineGovernanceServiceImpl;
import io.harness.governance.pipeline.service.evaluators.OnPipeline;
import io.harness.governance.pipeline.service.evaluators.OnWorkflow;
import io.harness.governance.pipeline.service.evaluators.PipelineStatusEvaluator;
import io.harness.governance.pipeline.service.evaluators.WorkflowStatusEvaluator;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesContainerServiceImpl;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitCheckerFactoryImpl;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.configuration.LimitConfigurationServiceMongo;
import io.harness.limits.counter.service.CounterService;
import io.harness.limits.counter.service.CounterServiceImpl;
import io.harness.limits.defaults.service.DefaultLimitsService;
import io.harness.limits.defaults.service.DefaultLimitsServiceImpl;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.lock.PersistentLocker;
import io.harness.logstreaming.LogStreamingServiceClientFactory;
import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.marketplace.gcp.procurement.CDProductHandler;
import io.harness.marketplace.gcp.procurement.GcpProductHandler;
import io.harness.mongo.MongoConfig;
import io.harness.notifications.AlertNotificationRuleChecker;
import io.harness.notifications.AlertNotificationRuleCheckerImpl;
import io.harness.notifications.AlertVisibilityChecker;
import io.harness.notifications.AlertVisibilityCheckerImpl;
import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.perpetualtask.PerpetualTaskServiceModule;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.queue.QueueController;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.redesign.services.CustomExecutionServiceImpl;
import io.harness.redis.RedisConfig;
import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.SchedulerConfig;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secretmanagers.SecretsManagerRBACService;
import io.harness.secretmanagers.SecretsManagerRBACServiceImpl;
import io.harness.secrets.SecretsAuditService;
import io.harness.secrets.SecretsAuditServiceImpl;
import io.harness.secrets.SecretsDelegateCacheService;
import io.harness.secrets.SecretsDelegateCacheServiceImpl;
import io.harness.secrets.SecretsFileService;
import io.harness.secrets.SecretsFileServiceImpl;
import io.harness.secrets.SecretsRBACService;
import io.harness.secrets.SecretsRBACServiceImpl;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;
import io.harness.secrets.setupusage.SecretSetupUsageBuilders;
import io.harness.secrets.setupusage.builders.ConfigFileSetupUsageBuilder;
import io.harness.secrets.setupusage.builders.SecretManagerSetupUsageBuilder;
import io.harness.secrets.setupusage.builders.ServiceVariableSetupUsageBuilder;
import io.harness.secrets.setupusage.builders.SettingAttributeSetupUsageBuilder;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.seeddata.SampleDataProviderService;
import io.harness.seeddata.SampleDataProviderServiceImpl;
import io.harness.serializer.YamlUtils;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.service.impl.DelegateCacheImpl;
import io.harness.service.intfc.DelegateCache;
import io.harness.templatizedsm.RuntimeCredentialsInjector;
import io.harness.threading.ThreadPool;
import io.harness.time.TimeModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;
import io.harness.version.VersionModule;

import software.wings.DataStorageMode;
import software.wings.alerts.AlertModule;
import software.wings.backgroundjobs.AccountBackgroundJobService;
import software.wings.backgroundjobs.AccountBackgroundJobServiceImpl;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Pipeline;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.Workflow;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.LoginSettingsServiceImpl;
import software.wings.beans.security.UserGroup;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.AwsClusterServiceImpl;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.cloudprovider.aws.AwsCodeDeployServiceImpl;
import software.wings.cloudprovider.aws.AwsLambdaService;
import software.wings.cloudprovider.aws.AwsLambdaServiceImpl;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.cloudprovider.aws.EcsContainerServiceImpl;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.features.ApiKeysFeature;
import software.wings.features.ApprovalFlowFeature;
import software.wings.features.AuditTrailFeature;
import software.wings.features.CeBudgetFeature;
import software.wings.features.CeCloudAccountFeature;
import software.wings.features.CeClusterFeature;
import software.wings.features.CustomDashboardFeature;
import software.wings.features.DelegatesFeature;
import software.wings.features.DeploymentHistoryFeature;
import software.wings.features.FlowControlFeature;
import software.wings.features.GitOpsFeature;
import software.wings.features.GovernanceFeature;
import software.wings.features.IpWhitelistingFeature;
import software.wings.features.JiraNotificationFeature;
import software.wings.features.LdapFeature;
import software.wings.features.PagerDutyNotificationFeature;
import software.wings.features.PipelineGovernanceFeature;
import software.wings.features.RbacFeature;
import software.wings.features.RestApiFeature;
import software.wings.features.SamlFeature;
import software.wings.features.SecretsManagementFeature;
import software.wings.features.ServiceNowNotificationFeature;
import software.wings.features.TagsFeature;
import software.wings.features.TemplateLibraryFeature;
import software.wings.features.TwoFactorAuthenticationFeature;
import software.wings.features.UsersFeature;
import software.wings.features.api.ApiBlocker;
import software.wings.features.api.Feature;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.FeatureService;
import software.wings.features.api.FeatureServiceImpl;
import software.wings.features.api.PremiumFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.features.api.RestrictedFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.graphql.utils.nameservice.NameServiceImpl;
import software.wings.helpers.ext.ami.AmiService;
import software.wings.helpers.ext.ami.AmiServiceImpl;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.helpers.ext.azure.AcrServiceImpl;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrClassicServiceImpl;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.ecr.EcrServiceImpl;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.GcbServiceImpl;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.gcs.GcsServiceImpl;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.HelmDeployServiceUnsupported;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerUnsupported;
import software.wings.helpers.ext.sftp.SftpService;
import software.wings.helpers.ext.sftp.SftpServiceImpl;
import software.wings.helpers.ext.smb.SmbService;
import software.wings.helpers.ext.smb.SmbServiceImpl;
import software.wings.helpers.ext.url.SubdomainUrlHelper;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.licensing.DatabaseLicenseProviderImpl;
import software.wings.licensing.LicenseProvider;
import software.wings.licensing.LicenseService;
import software.wings.licensing.LicenseServiceImpl;
import software.wings.ratelimit.DelegateRequestRateLimiter;
import software.wings.resources.graphql.GraphQLRateLimiter;
import software.wings.resources.graphql.GraphQLUtils;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.scheduler.ServiceJobScheduler;
import software.wings.scim.ScimGroupService;
import software.wings.scim.ScimGroupServiceImpl;
import software.wings.scim.ScimUserService;
import software.wings.scim.ScimUserServiceImpl;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountChecker;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountCheckerImpl;
import software.wings.security.saml.SamlUserGroupSync;
import software.wings.service.DelegateTaskExecutor;
import software.wings.service.EcrClassicBuildServiceImpl;
import software.wings.service.impl.AccountServiceImpl;
import software.wings.service.impl.AcrBuildServiceImpl;
import software.wings.service.impl.ActivityServiceImpl;
import software.wings.service.impl.AlertNotificationRuleServiceImpl;
import software.wings.service.impl.AlertServiceImpl;
import software.wings.service.impl.AmiBuildServiceImpl;
import software.wings.service.impl.ApiKeyServiceImpl;
import software.wings.service.impl.AppContainerServiceImpl;
import software.wings.service.impl.AppServiceImpl;
import software.wings.service.impl.ApplicationManifestServiceImpl;
import software.wings.service.impl.ApprovalPolingServiceImpl;
import software.wings.service.impl.ArtifactStreamServiceBindingServiceImpl;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.impl.AssignDelegateServiceImpl;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.AwsHelperResourceServiceImpl;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.AwsMarketPlaceApiHandlerImpl;
import software.wings.service.impl.AzureInfrastructureProvider;
import software.wings.service.impl.AzureMachineImageBuildServiceImpl;
import software.wings.service.impl.AzureResourceServiceImpl;
import software.wings.service.impl.BarrierServiceImpl;
import software.wings.service.impl.BuildSourceServiceImpl;
import software.wings.service.impl.CatalogServiceImpl;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.CommandServiceImpl;
import software.wings.service.impl.ConfigServiceImpl;
import software.wings.service.impl.DelegateProfileManagerServiceImpl;
import software.wings.service.impl.DelegateProfileServiceImpl;
import software.wings.service.impl.DelegateScopeServiceImpl;
import software.wings.service.impl.DelegateSelectionLogsServiceImpl;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.DownloadTokenServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.EmailNotificationServiceImpl;
import software.wings.service.impl.EntityVersionServiceImpl;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.ExternalApiRateLimitingServiceImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.GcpInfrastructureProvider;
import software.wings.service.impl.GcrBuildServiceImpl;
import software.wings.service.impl.GcsBuildServiceImpl;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.HarnessApiKeyServiceImpl;
import software.wings.service.impl.HarnessSampleAppServiceImpl;
import software.wings.service.impl.HarnessTagServiceImpl;
import software.wings.service.impl.HarnessUserGroupServiceImpl;
import software.wings.service.impl.HostServiceImpl;
import software.wings.service.impl.InfrastructureMappingServiceImpl;
import software.wings.service.impl.InfrastructureProvisionerServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.impl.MicrosoftTeamsNotificationServiceImpl;
import software.wings.service.impl.MigrationServiceImpl;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.NotificationDispatcherServiceImpl;
import software.wings.service.impl.NotificationServiceImpl;
import software.wings.service.impl.NotificationSetupServiceImpl;
import software.wings.service.impl.PagerDutyServiceImpl;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.impl.PipelineServiceImpl;
import software.wings.service.impl.PluginServiceImpl;
import software.wings.service.impl.PreferenceServiceImpl;
import software.wings.service.impl.ResourceConstraintServiceImpl;
import software.wings.service.impl.ResourceLookupServiceImpl;
import software.wings.service.impl.RoleServiceImpl;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.impl.ServiceInstanceServiceImpl;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.impl.ServiceVariableServiceImpl;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.SftpBuildServiceImpl;
import software.wings.service.impl.SlackMessageSenderImpl;
import software.wings.service.impl.SlackNotificationServiceImpl;
import software.wings.service.impl.SmbBuildServiceImpl;
import software.wings.service.impl.StateExecutionServiceImpl;
import software.wings.service.impl.StaticInfrastructureProvider;
import software.wings.service.impl.StatisticsServiceImpl;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.impl.SystemCatalogSeviceImpl;
import software.wings.service.impl.UsageRestrictionsServiceImpl;
import software.wings.service.impl.UserGroupServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.WebHookServiceImpl;
import software.wings.service.impl.WhitelistServiceImpl;
import software.wings.service.impl.WorkflowExecutionBaselineServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.alert.NotificationRulesStatusServiceImpl;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.ExperimentalAnalysisServiceImpl;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecordServiceImpl;
import software.wings.service.impl.analysis.LogLabelingServiceImpl;
import software.wings.service.impl.analysis.MetricDataAnalysisServiceImpl;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecordServiceImpl;
import software.wings.service.impl.analysis.VerificationServiceImpl;
import software.wings.service.impl.apm.ApmVerificationServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsServiceImpl;
import software.wings.service.impl.applicationmanifest.HelmChartServiceImpl;
import software.wings.service.impl.artifact.ArtifactCleanupServiceAsyncImpl;
import software.wings.service.impl.artifact.ArtifactCollectionServiceAsyncImpl;
import software.wings.service.impl.artifact.ArtifactCollectionServiceImpl;
import software.wings.service.impl.artifact.ArtifactServiceImpl;
import software.wings.service.impl.artifact.CustomBuildSourceServiceImpl;
import software.wings.service.impl.aws.delegate.AwsEcrHelperServiceDelegateImpl;
import software.wings.service.impl.aws.manager.AwsAsgHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsCFHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsCodeDeployHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsEc2HelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsEcrHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsEcsHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsElbHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsIamHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsLambdaHelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsRoute53HelperServiceManagerImpl;
import software.wings.service.impl.aws.manager.AwsS3HelperServiceManagerImpl;
import software.wings.service.impl.azure.manager.AzureARMManagerImpl;
import software.wings.service.impl.azure.manager.AzureAppServiceManagerImpl;
import software.wings.service.impl.azure.manager.AzureVMSSHelperServiceManagerImpl;
import software.wings.service.impl.ce.CeAccountExpirationCheckerImpl;
import software.wings.service.impl.compliance.GovernanceConfigServiceImpl;
import software.wings.service.impl.customdeployment.CustomDeploymentTypeServiceImpl;
import software.wings.service.impl.datadog.DatadogServiceImpl;
import software.wings.service.impl.deployment.checks.AccountExpirationChecker;
import software.wings.service.impl.deployment.checks.DeploymentRateLimitChecker;
import software.wings.service.impl.deployment.checks.SIUsageChecker;
import software.wings.service.impl.dynatrace.DynaTraceServiceImpl;
import software.wings.service.impl.elk.ElkAnalysisServiceImpl;
import software.wings.service.impl.expression.ExpressionBuilderServiceImpl;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.impl.infra.InfraDownloadServiceImpl;
import software.wings.service.impl.infrastructuredefinition.InfrastructureDefinitionServiceImpl;
import software.wings.service.impl.instana.InstanaServiceImpl;
import software.wings.service.impl.instance.CloudToHarnessMappingServiceImpl;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl;
import software.wings.service.impl.instance.DeploymentServiceImpl;
import software.wings.service.impl.instance.InstanceHandlerFactory;
import software.wings.service.impl.instance.InstanceHandlerFactoryService;
import software.wings.service.impl.instance.InstanceServiceImpl;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskService;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskServiceImpl;
import software.wings.service.impl.instance.ServerlessDashboardServiceImpl;
import software.wings.service.impl.instance.ServerlessInstanceServiceImpl;
import software.wings.service.impl.instance.licensing.InstanceLimitProviderImpl;
import software.wings.service.impl.instance.licensing.InstanceUsageLimitCheckerImpl;
import software.wings.service.impl.instance.licensing.InstanceUsageLimitExcessHandlerImpl;
import software.wings.service.impl.instance.limits.LimitVicinityHandlerImpl;
import software.wings.service.impl.instance.stats.InstanceStatServiceImpl;
import software.wings.service.impl.instance.stats.ServerlessInstanceStatServiceImpl;
import software.wings.service.impl.instance.stats.collector.StatsCollectorImpl;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.ContainerSyncImpl;
import software.wings.service.impl.marketplace.MarketPlaceServiceImpl;
import software.wings.service.impl.newrelic.NewRelicServiceImpl;
import software.wings.service.impl.notifications.NotificationDispatcher;
import software.wings.service.impl.notifications.NotificationGroupBasedDispatcher;
import software.wings.service.impl.notifications.UseNotificationGroup;
import software.wings.service.impl.notifications.UseUserGroup;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.impl.personalization.PersonalizationServiceImpl;
import software.wings.service.impl.prometheus.PrometheusAnalysisServiceImpl;
import software.wings.service.impl.scalyr.ScalyrServiceImpl;
import software.wings.service.impl.security.AwsSecretsManagerServiceImpl;
import software.wings.service.impl.security.AzureSecretsManagerServiceImpl;
import software.wings.service.impl.security.CyberArkServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.GcpSecretsManagerServiceImpl;
import software.wings.service.impl.security.GcpSecretsManagerServiceV2Impl;
import software.wings.service.impl.security.KmsServiceImpl;
import software.wings.service.impl.security.LocalSecretManagerServiceImpl;
import software.wings.service.impl.security.ManagerDecryptionServiceImpl;
import software.wings.service.impl.security.NGSecretManagerServiceImpl;
import software.wings.service.impl.security.SecretDecryptionServiceImpl;
import software.wings.service.impl.security.SecretManagerConfigServiceImpl;
import software.wings.service.impl.security.SecretManagerImpl;
import software.wings.service.impl.security.VaultServiceImpl;
import software.wings.service.impl.security.customsecretsmanager.CustomEncryptedDataDetailBuilderImpl;
import software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerServiceImpl;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl;
import software.wings.service.impl.splunk.SplunkAnalysisServiceImpl;
import software.wings.service.impl.stackdriver.StackDriverServiceImpl;
import software.wings.service.impl.sumo.SumoLogicAnalysisServiceImpl;
import software.wings.service.impl.trigger.TriggerExecutionServiceImpl;
import software.wings.service.impl.trigger.TriggerServiceImpl;
import software.wings.service.impl.verification.CV24x7DashboardServiceImpl;
import software.wings.service.impl.verification.CVActivityLogServiceImpl;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
import software.wings.service.impl.verification.CvValidationService;
import software.wings.service.impl.verification.CvValidationServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.impl.yaml.AppYamlResourceServiceImpl;
import software.wings.service.impl.yaml.EntityUpdateServiceImpl;
import software.wings.service.impl.yaml.GitClientUnsupported;
import software.wings.service.impl.yaml.GitSyncServiceImpl;
import software.wings.service.impl.yaml.K8sGlobalConfigServiceUnsupported;
import software.wings.service.impl.yaml.YamlArtifactStreamServiceImpl;
import software.wings.service.impl.yaml.YamlChangeSetServiceImpl;
import software.wings.service.impl.yaml.YamlDirectoryServiceImpl;
import software.wings.service.impl.yaml.YamlGitServiceImpl;
import software.wings.service.impl.yaml.YamlHistoryServiceImpl;
import software.wings.service.impl.yaml.YamlResourceServiceImpl;
import software.wings.service.impl.yaml.YamlSuccessfulChangeServiceImpl;
import software.wings.service.impl.yaml.service.YamlCloneServiceImpl;
import software.wings.service.impl.yaml.sync.GitSyncErrorServiceImpl;
import software.wings.service.impl.yaml.sync.YamlGitConfigServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AcrBuildService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AmiBuildService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ApprovalPolingService;
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AwsHelperResourceService;
import software.wings.service.intfc.AwsMarketPlaceApiHandler;
import software.wings.service.intfc.AzureArtifactsBuildService;
import software.wings.service.intfc.AzureMachineImageBuildService;
import software.wings.service.intfc.AzureResourceService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.DelegateProfileManagerService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.EcrClassicBuildService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ErrorReporter;
import software.wings.service.intfc.ExternalApiRateLimitingService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.GcsBuildService;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.service.intfc.HarnessSampleAppService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.MicrosoftTeamsNotificationService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PermitService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.PluginService;
import software.wings.service.intfc.PreferenceService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SftpBuildService;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.SlackMessageSender;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.service.intfc.SmbBuildService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.VerificationService;
import software.wings.service.intfc.WebHookService;
import software.wings.service.intfc.WhitelistService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.alert.NotificationRulesStatusService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ExperimentalAnalysisService;
import software.wings.service.intfc.analysis.ExperimentalMetricAnalysisRecordService;
import software.wings.service.intfc.analysis.LogLabelingService;
import software.wings.service.intfc.analysis.LogVerificationService;
import software.wings.service.intfc.analysis.LogVerificationServiceImpl;
import software.wings.service.intfc.analysis.TimeSeriesMLAnalysisRecordService;
import software.wings.service.intfc.apm.ApmVerificationService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.service.intfc.artifact.CustomBuildSourceService;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsCFHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEcrHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEcsHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsElbHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsLambdaHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsRoute53HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsS3HelperServiceManager;
import software.wings.service.intfc.azure.manager.AzureARMManager;
import software.wings.service.intfc.azure.manager.AzureAppServiceManager;
import software.wings.service.intfc.azure.manager.AzureVMSSHelperServiceManager;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.datadog.DatadogService;
import software.wings.service.intfc.deployment.AccountExpiryCheck;
import software.wings.service.intfc.deployment.PreDeploymentChecker;
import software.wings.service.intfc.deployment.RateLimitCheck;
import software.wings.service.intfc.deployment.ServiceInstanceUsage;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.service.intfc.instana.InstanaService;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.instance.ServerlessDashboardService;
import software.wings.service.intfc.instance.ServerlessInstanceService;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.instance.stats.ServerlessInstanceStatService;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;
import software.wings.service.intfc.limits.LimitVicinityHandler;
import software.wings.service.intfc.marketplace.MarketPlaceService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.pagerduty.PagerDutyService;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;
import software.wings.service.intfc.scalyr.ScalyrService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CustomEncryptedDataDetailBuilder;
import software.wings.service.intfc.security.CustomSecretsManagerService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.GcpSecretsManagerServiceV2;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalSecretManagerService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.NGSecretFileService;
import software.wings.service.intfc.security.NGSecretFileServiceImpl;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.service.intfc.security.NGSecretServiceImpl;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.service.intfc.servicenow.ServiceNowService;
import software.wings.service.intfc.splunk.SplunkAnalysisService;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.service.intfc.trigger.TriggerExecutionService;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.YamlSuccessfulChangeService;
import software.wings.service.intfc.yaml.clone.YamlCloneService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.service.intfc.yaml.sync.YamlGitConfigService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.signup.BugsnagErrorReporter;
import software.wings.signup.SignupServiceImpl;
import software.wings.sm.ExpressionProcessorFactory;
import software.wings.utils.CdnStorageUrlGenerator;
import software.wings.utils.HostValidationService;
import software.wings.utils.HostValidationServiceImpl;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.dropwizard.lifecycle.Managed;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Guice Module for initializing all beans.
 *
 * @author Rishi
 */
@Slf4j
public class WingsModule extends AbstractModule implements ServersModule {
  private final String hashicorpvault = "hashicorpvault";
  private final MainConfiguration configuration;

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration Dropwizard configuration
   */
  public WingsModule(MainConfiguration configuration) {
    this.configuration = configuration;
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return Suppliers.memoize(() -> getDelegateCallbackToken(delegateServiceGrpcClient, configuration));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, MainConfiguration appConfig) {
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("ngManager")
                                  .setConnection(appConfig.getMongoConnectionFactory().getUri())
                                  .build())
            .build());
    log.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return configuration.getDistributedLockImplementation() == null ? MONGO
                                                                    : configuration.getDistributedLockImplementation();
  }

  @Provides
  @Singleton
  MongoConfig mongoConfig() {
    return configuration.getMongoConnectionFactory();
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisLockConfig() {
    return configuration.getRedisLockConfig();
  }

  @Provides
  @Named("atmosphere")
  @Singleton
  RedisConfig redisAtmoshphereConfig() {
    return configuration.getRedisAtmosphereConfig();
  }

  @Provides
  public UrlConfiguration urlConfiguration() {
    return new UrlConfiguration(configuration.getPortal().getUrl(), configuration.getApiUrl(),
        configuration.getDelegateMetadataUrl(), configuration.getWatcherMetadataUrl());
  }

  @Provides
  public DelegateGrpcConfig delegateGrpcConnfig() {
    return new DelegateGrpcConfig(configuration.getDelegateGrpcServicePort());
  }

  @Provides
  public BugsnagErrorReporterConfiguration bugsnagErrorReporterConfiguration() {
    return new BugsnagErrorReporterConfiguration(
        configuration.isTrialRegistrationAllowed(), configuration.getBugsnagApiKey());
  }

  @Provides
  @Singleton
  public JobsFrequencyConfig jobsFrequencyConfig() {
    return configuration.getJobsFrequencyConfig();
  }

  @Provides
  @Singleton
  public CdnStorageUrlGenerator cdnStorageUrlGenerator() {
    String clusterType = System.getenv("CLUSTER_TYPE");
    boolean isFreeCluster = StringUtils.equals(clusterType, "freemium");

    return new CdnStorageUrlGenerator(configuration.getCdnConfig(), isFreeCluster);
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());
    install(TimeModule.getInstance());
    install(OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                                .serviceName("CD")
                                                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                                .build()));
    install(DelegateServiceDriverModule.getInstance());
    install(new DelegateServiceDriverGrpcClientModule(configuration.getPortal().getJwtNextGenManagerSecret(),
        configuration.getGrpcDelegateServiceClientConfig().getTarget(),
        configuration.getGrpcDelegateServiceClientConfig().getAuthority()));
    install(PersistentLockModule.getInstance());
    install(AlertModule.getInstance());

    install(new AbstractModule() {
      @Override
      protected void configure() {
        RedisConfig redisConfig = configuration.getEventsFrameworkConfiguration().getRedisConfig();
        if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
              .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
              .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
              .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
        } else {
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
              .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_CRUD, redisConfig,
                  EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE));
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
              .toInstance(RedisProducer.of(EventsFrameworkConstants.FEATURE_FLAG_STREAM, redisConfig,
                  EventsFrameworkConstants.FEATURE_FLAG_MAX_TOPIC_SIZE));
          bind(Producer.class)
              .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
              .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_ACTIVITY, redisConfig,
                  EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_TOPIC_SIZE));
        }
      }
    });
    install(FeatureFlagModule.getInstance());

    bind(MainConfiguration.class).toInstance(configuration);
    // RetryOnException Binding start
    bind(MethodExecutionHelper.class); // untargetted binding for eager loading
    RetryOnExceptionInterceptor retryOnExceptionInterceptor = new RetryOnExceptionInterceptor();
    requestInjection(retryOnExceptionInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(RetryOnException.class), retryOnExceptionInterceptor);
    // Retry On Exception Binding ends
    bind(SchedulerConfig.class)
        .annotatedWith(Names.named("BackgroundSchedule"))
        .toInstance(configuration.getBackgroundSchedulerConfig());
    bind(PipelineConfig.class).toInstance(configuration.getPipelineConfig());
    bind(QueueController.class).to(ConfigurationController.class);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(AppService.class).to(AppServiceImpl.class);
    bind(HarnessSampleAppService.class).to(HarnessSampleAppServiceImpl.class);
    bind(SampleDataProviderService.class).to(SampleDataProviderServiceImpl.class);
    bind(ApplicationManifestService.class).to(ApplicationManifestServiceImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(AuditService.class).to(AuditServiceImpl.class);
    bind(ResourceLookupService.class).to(ResourceLookupServiceImpl.class);
    bind(ArtifactStreamService.class).to(ArtifactStreamServiceImpl.class);
    bind(ArtifactStreamServiceBindingService.class).to(ArtifactStreamServiceBindingServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(SignupService.class).to(SignupServiceImpl.class);
    bind(UserGroupService.class).to(UserGroupServiceImpl.class);
    bind(RoleService.class).to(RoleServiceImpl.class);
    bind(ServiceResourceService.class).to(ServiceResourceServiceImpl.class);
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
    bind(ServiceTemplateService.class).to(ServiceTemplateServiceImpl.class);
    bind(WorkflowService.class).to(WorkflowServiceImpl.class);
    bind(WorkflowExecutionService.class).to(WorkflowExecutionServiceImpl.class);
    bind(SweepingOutputService.class).to(SweepingOutputServiceImpl.class);
    bind(StateExecutionService.class).to(StateExecutionServiceImpl.class);
    bind(ConfigService.class).to(ConfigServiceImpl.class);
    bind(AppContainerService.class).to(AppContainerServiceImpl.class);
    bind(CatalogService.class).to(CatalogServiceImpl.class);
    bind(HostService.class).to(HostServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
    bind(GcsService.class).to(GcsServiceImpl.class);
    bind(GcsBuildService.class).to(GcsBuildServiceImpl.class);
    bind(SettingsService.class).to(SettingsServiceImpl.class);
    bind(ExpressionProcessorFactory.class).to(WingsExpressionProcessorFactory.class);
    bind(EmailNotificationService.class).to(EmailNotificationServiceImpl.class);
    bind(ServiceInstanceService.class).to(ServiceInstanceServiceImpl.class);
    bind(ActivityService.class).to(ActivityServiceImpl.class);
    bind(LogService.class).to(LogServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(StatisticsService.class).to(StatisticsServiceImpl.class);
    bind(DashboardStatisticsService.class).to(DashboardStatisticsServiceImpl.class);
    bind(InstanceService.class).to(InstanceServiceImpl.class);
    bind(InstanceSyncPerpetualTaskService.class).to(InstanceSyncPerpetualTaskServiceImpl.class);
    bind(InstanceHandlerFactoryService.class).to(InstanceHandlerFactory.class);
    bind(BuildSourceService.class).to(BuildSourceServiceImpl.class);
    bind(ServiceVariableService.class).to(ServiceVariableServiceImpl.class);
    bind(AccountService.class).to(AccountServiceImpl.class);
    bind(AdminAccountService.class).to(AdminAccountServiceImpl.class);
    bind(AdminUserService.class).to(AdminUserServiceImpl.class);
    bind(AdminFeatureFlagService.class).to(AdminFeatureFlagServiceImpl.class);
    bind(AccountSummaryHelper.class).to(AccountSummaryHelperImpl.class);
    bind(PipelineService.class).to(PipelineServiceImpl.class);
    bind(NotificationSetupService.class).to(NotificationSetupServiceImpl.class);
    bind(NotificationDispatcherService.class).to(NotificationDispatcherServiceImpl.class);
    bind(ServiceClassLocator.class);
    bind(EntityVersionService.class).to(EntityVersionServiceImpl.class);
    bind(PluginService.class).to(PluginServiceImpl.class);
    bind(CommandService.class).to(CommandServiceImpl.class);
    bind(DelegateService.class).to(DelegateServiceImpl.class);
    bind(DelegateScopeService.class).to(DelegateScopeServiceImpl.class);
    bind(DelegateSelectionLogsService.class).to(DelegateSelectionLogsServiceImpl.class);
    bind(BarrierService.class).to(BarrierServiceImpl.class);
    bind(DownloadTokenService.class).to(DownloadTokenServiceImpl.class);
    bind(CloudWatchService.class).to(CloudWatchServiceImpl.class);
    bind(SlackNotificationService.class).to(SlackNotificationServiceImpl.class);
    bind(MicrosoftTeamsNotificationService.class).to(MicrosoftTeamsNotificationServiceImpl.class);
    bind(EcsContainerService.class).to(EcsContainerServiceImpl.class);
    bind(AwsClusterService.class).to(AwsClusterServiceImpl.class);
    bind(GkeClusterService.class).to(GkeClusterServiceImpl.class);
    bind(KubernetesContainerService.class).to(KubernetesContainerServiceImpl.class);
    bind(InfrastructureMappingService.class).to(InfrastructureMappingServiceImpl.class);
    bind(InfrastructureDefinitionService.class).to(InfrastructureDefinitionServiceImpl.class);
    bind(InfrastructureProvisionerService.class).to(InfrastructureProvisionerServiceImpl.class);
    bind(ResourceConstraintService.class).to(ResourceConstraintServiceImpl.class);
    bind(LicenseService.class).to(LicenseServiceImpl.class);
    bind(AccountBackgroundJobService.class).to(AccountBackgroundJobServiceImpl.class);
    bind(LicenseProvider.class).to(DatabaseLicenseProviderImpl.class);
    bind(AppdynamicsService.class).to(AppdynamicsServiceImpl.class);
    bind(InstanaService.class).to(InstanaServiceImpl.class);
    bind(PersonalizationService.class).to(PersonalizationServiceImpl.class);
    bind(StackDriverService.class).to(StackDriverServiceImpl.class);
    bind(NewRelicService.class).to(NewRelicServiceImpl.class);
    bind(DynaTraceService.class).to(DynaTraceServiceImpl.class);
    bind(DatadogService.class).to(DatadogServiceImpl.class);
    bind(MetricDataAnalysisService.class).to(MetricDataAnalysisServiceImpl.class);
    bind(AnalysisService.class).to(AnalysisServiceImpl.class);
    bind(ExperimentalAnalysisService.class).to(ExperimentalAnalysisServiceImpl.class);
    bind(ContinuousVerificationService.class).to(ContinuousVerificationServiceImpl.class);
    bind(CV24x7DashboardService.class).to(CV24x7DashboardServiceImpl.class);
    bind(ElkAnalysisService.class).to(ElkAnalysisServiceImpl.class);
    bind(PrometheusAnalysisService.class).to(PrometheusAnalysisServiceImpl.class);
    bind(SplunkAnalysisService.class).to(SplunkAnalysisServiceImpl.class);
    bind(SystemCatalogService.class).to(SystemCatalogSeviceImpl.class);
    bind(AwsCodeDeployService.class).to(AwsCodeDeployServiceImpl.class);
    bind(EcrBuildService.class).to(EcrBuildServiceImpl.class);
    bind(EcrClassicBuildService.class).to(EcrClassicBuildServiceImpl.class);
    bind(EcrService.class).to(EcrServiceImpl.class);
    bind(EcrService.class).to(EcrServiceImpl.class);
    bind(AwsEcrHelperServiceDelegate.class).to(AwsEcrHelperServiceDelegateImpl.class);
    bind(EcrClassicService.class).to(EcrClassicServiceImpl.class);
    bind(GcrApiService.class).to(GcrApiServiceImpl.class);
    bind(GcrBuildService.class).to(GcrBuildServiceImpl.class);
    bind(AcrService.class).to(AcrServiceImpl.class);
    bind(AcrBuildService.class).to(AcrBuildServiceImpl.class);
    bind(AmiService.class).to(AmiServiceImpl.class);
    bind(AmiBuildService.class).to(AmiBuildServiceImpl.class);
    bind(AwsHelperResourceService.class).to(AwsHelperResourceServiceImpl.class);
    bind(AzureResourceService.class).to(AzureResourceServiceImpl.class);
    bind(AzureMachineImageBuildService.class).to(AzureMachineImageBuildServiceImpl.class);
    bind(AssignDelegateService.class).to(AssignDelegateServiceImpl.class);
    bind(DelegateCache.class).to(DelegateCacheImpl.class);
    bind(ExpressionBuilderService.class).to(ExpressionBuilderServiceImpl.class);
    bind(HostValidationService.class).to(HostValidationServiceImpl.class);
    bind(WebHookService.class).to(WebHookServiceImpl.class);
    bind(YamlHistoryService.class).to(YamlHistoryServiceImpl.class);
    bind(YamlDirectoryService.class).to(YamlDirectoryServiceImpl.class);
    bind(YamlResourceService.class).to(YamlResourceServiceImpl.class);
    bind(YamlCloneService.class).to(YamlCloneServiceImpl.class);
    bind(AppYamlResourceService.class).to(AppYamlResourceServiceImpl.class);
    bind(YamlGitService.class).to(YamlGitServiceImpl.class);
    bind(YamlArtifactStreamService.class).to(YamlArtifactStreamServiceImpl.class);
    bind(EntityUpdateService.class).to(EntityUpdateServiceImpl.class);
    bind(HarnessTagService.class).to(HarnessTagServiceImpl.class);
    bind(AlertService.class).to(AlertServiceImpl.class).in(Singleton.class);
    bind(AlertNotificationRuleService.class).to(AlertNotificationRuleServiceImpl.class);
    bind(YamlChangeSetService.class).to(YamlChangeSetServiceImpl.class);
    bind(YamlSuccessfulChangeService.class).to(YamlSuccessfulChangeServiceImpl.class);
    bind(TriggerService.class).to(TriggerServiceImpl.class);
    bind(VerificationService.class).to(VerificationServiceImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(MigrationService.class).to(MigrationServiceImpl.class).in(Singleton.class);
    bind(WorkflowExecutionBaselineService.class).to(WorkflowExecutionBaselineServiceImpl.class);
    bind(GitClient.class).to(GitClientUnsupported.class).in(Singleton.class);
    bind(WhitelistService.class).to(WhitelistServiceImpl.class);
    bind(ApiKeyService.class).to(ApiKeyServiceImpl.class);
    bind(ExternalApiRateLimitingService.class).to(ExternalApiRateLimitingServiceImpl.class);
    bind(PreferenceService.class).to(PreferenceServiceImpl.class);
    bind(HarnessUserGroupService.class).to(HarnessUserGroupServiceImpl.class);
    bind(InfraDownloadService.class).to(InfraDownloadServiceImpl.class);
    bind(AwsEcrHelperServiceManager.class).to(AwsEcrHelperServiceManagerImpl.class);
    bind(UsageRestrictionsService.class).to(UsageRestrictionsServiceImpl.class);
    bind(AwsElbHelperServiceManager.class).to(AwsElbHelperServiceManagerImpl.class);
    bind(AwsEcsHelperServiceManager.class).to(AwsEcsHelperServiceManagerImpl.class);
    bind(AwsIamHelperServiceManager.class).to(AwsIamHelperServiceManagerImpl.class);
    bind(AwsEc2HelperServiceManager.class).to(AwsEc2HelperServiceManagerImpl.class);
    bind(AwsAsgHelperServiceManager.class).to(AwsAsgHelperServiceManagerImpl.class);
    bind(AwsCodeDeployHelperServiceManager.class).to(AwsCodeDeployHelperServiceManagerImpl.class);
    bind(AwsLambdaHelperServiceManager.class).to(AwsLambdaHelperServiceManagerImpl.class);
    bind(AzureVMSSHelperServiceManager.class).to(AzureVMSSHelperServiceManagerImpl.class);
    bind(AzureAppServiceManager.class).to(AzureAppServiceManagerImpl.class);
    bind(AzureARMManager.class).to(AzureARMManagerImpl.class);
    bind(DelegateProfileService.class).to(DelegateProfileServiceImpl.class);
    bind(DelegateProfileManagerService.class).to(DelegateProfileManagerServiceImpl.class);
    bind(AwsCFHelperServiceManager.class).to(AwsCFHelperServiceManagerImpl.class);
    bind(SumoLogicAnalysisService.class).to(SumoLogicAnalysisServiceImpl.class);
    bind(InstanceStatService.class).to(InstanceStatServiceImpl.class);
    bind(StatsCollector.class).to(StatsCollectorImpl.class);
    bind(LogLabelingService.class).to(LogLabelingServiceImpl.class);
    bind(AwsRoute53HelperServiceManager.class).to(AwsRoute53HelperServiceManagerImpl.class);
    bind(HarnessApiKeyService.class).to(HarnessApiKeyServiceImpl.class);
    bind(K8sGlobalConfigService.class).to(K8sGlobalConfigServiceUnsupported.class);
    bind(SlackMessageSender.class).to(SlackMessageSenderImpl.class);
    bind(AwsS3HelperServiceManager.class).to(AwsS3HelperServiceManagerImpl.class);
    bind(GitSyncErrorService.class).to(GitSyncErrorServiceImpl.class);
    bind(YamlGitConfigService.class).to(YamlGitConfigServiceImpl.class);
    bind(ErrorReporter.class).to(BugsnagErrorReporter.class);
    bind(CeAccountExpirationChecker.class).to(CeAccountExpirationCheckerImpl.class);

    bind(GcbService.class).to(GcbServiceImpl.class);

    bind(GraphQLRateLimiter.class);
    bind(GraphQLUtils.class);
    bind(DelegateRequestRateLimiter.class);
    bind(SamlUserGroupSync.class);
    bind(ScimUserService.class).to(ScimUserServiceImpl.class);
    bind(ScimGroupService.class).to(ScimGroupServiceImpl.class);
    bind(SmbService.class).to(SmbServiceImpl.class);
    bind(SmbBuildService.class).to(SmbBuildServiceImpl.class);
    bind(SftpService.class).to(SftpServiceImpl.class);
    bind(SftpBuildService.class).to(SftpBuildServiceImpl.class);

    bind(LoginSettingsService.class).to(LoginSettingsServiceImpl.class);
    bind(CCMSettingService.class).to(CCMSettingServiceImpl.class);
    bind(ClusterRecordService.class).to(ClusterRecordServiceImpl.class);
    bind(BudgetService.class).to(BudgetServiceImpl.class);
    bind(ViewCustomFieldService.class).to(ViewCustomFieldServiceImpl.class);
    bind(ViewsBillingService.class).to(ViewsBillingServiceImpl.class);
    bind(CEViewService.class).to(CEViewServiceImpl.class);
    bind(CECommunicationsService.class).to(CECommunicationsServiceImpl.class);
    bind(CESlackWebhookService.class).to(CESlackWebhookServiceImpl.class);
    bind(CEReportScheduleService.class).to(CEReportScheduleServiceImpl.class);
    bind(CEReportTemplateBuilderService.class).to(CEReportTemplateBuilderServiceImpl.class);
    bind(HealthStatusService.class).to(HealthStatusServiceImpl.class);
    bind(GcpBillingService.class).to(GcpBillingServiceImpl.class);
    bind(PreAggregateBillingService.class).to(PreAggregateBillingServiceImpl.class);

    bind(WingsMongoExportImport.class);

    bind(InstanceUsageLimitChecker.class).to(InstanceUsageLimitCheckerImpl.class);
    bind(InstanceUsageLimitExcessHandler.class).to(InstanceUsageLimitExcessHandlerImpl.class);
    bind(InstanceLimitProvider.class).to(InstanceLimitProviderImpl.class);

    bind(NotificationRulesStatusService.class).to(NotificationRulesStatusServiceImpl.class);
    bind(TimeSeriesMLAnalysisRecordService.class).to(TimeSeriesMLAnalysisRecordServiceImpl.class);
    bind(ExperimentalMetricAnalysisRecordService.class).to(ExperimentalMetricAnalysisRecordServiceImpl.class);
    bind(GitSyncService.class).to(GitSyncServiceImpl.class);
    bind(SecretDecryptionService.class).to(SecretDecryptionServiceImpl.class);
    bind(CVNGVerificationTaskService.class).to(CVNGVerificationTaskServiceImpl.class);
    bind(CVNGService.class).to(CVNGServiceImpl.class);

    MapBinder<String, InfrastructureProvider> infrastructureProviderMapBinder =
        MapBinder.newMapBinder(binder(), String.class, InfrastructureProvider.class);
    infrastructureProviderMapBinder.addBinding(SettingVariableTypes.AWS.name()).to(AwsInfrastructureProvider.class);
    infrastructureProviderMapBinder.addBinding(SettingVariableTypes.AZURE.name()).to(AzureInfrastructureProvider.class);
    infrastructureProviderMapBinder.addBinding(SettingVariableTypes.GCP.name()).to(GcpInfrastructureProvider.class);
    infrastructureProviderMapBinder.addBinding(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        .to(StaticInfrastructureProvider.class);

    MapBinder<Class<? extends SettingValue>, Class<? extends BuildService>> buildServiceMapBinder =
        MapBinder.newMapBinder(binder(), new TypeLiteral<Class<? extends SettingValue>>() {},
            new TypeLiteral<Class<? extends BuildService>>() {});

    buildServiceMapBinder.addBinding(JenkinsConfig.class).toInstance(JenkinsBuildService.class);
    buildServiceMapBinder.addBinding(BambooConfig.class).toInstance(BambooBuildService.class);
    buildServiceMapBinder.addBinding(DockerConfig.class).toInstance(DockerBuildService.class);
    buildServiceMapBinder.addBinding(AwsConfig.class).toInstance(EcrBuildService.class);
    buildServiceMapBinder.addBinding(EcrConfig.class).toInstance(EcrClassicBuildService.class);
    buildServiceMapBinder.addBinding(GcpConfig.class).toInstance(GcrBuildService.class);
    buildServiceMapBinder.addBinding(AzureConfig.class).toInstance(AcrBuildService.class);
    buildServiceMapBinder.addBinding(NexusConfig.class).toInstance(NexusBuildService.class);
    buildServiceMapBinder.addBinding(ArtifactoryConfig.class).toInstance(ArtifactoryBuildService.class);
    buildServiceMapBinder.addBinding(SmbConfig.class).toInstance(SmbBuildService.class);
    buildServiceMapBinder.addBinding(SftpConfig.class).toInstance(SftpBuildService.class);
    buildServiceMapBinder.addBinding(AzureArtifactsPATConfig.class).toInstance(AzureArtifactsBuildService.class);

    install(new ManagerCacheRegistrar());
    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    install(SecretManagementCoreModule.getInstance());
    registerSecretManagementBindings();

    bind(PersistentScheduler.class)
        .annotatedWith(Names.named("BackgroundJobScheduler"))
        .to(BackgroundJobScheduler.class)
        .asEagerSingleton();

    if (Objects.equals(configuration.getBackgroundSchedulerConfig().getSchedulerName(),
            configuration.getServiceSchedulerConfig().getSchedulerName())) {
      bind(PersistentScheduler.class)
          .annotatedWith(Names.named("ServiceJobScheduler"))
          .to(BackgroundJobScheduler.class)
          .asEagerSingleton();
    } else {
      bind(PersistentScheduler.class)
          .annotatedWith(Names.named("ServiceJobScheduler"))
          .to(ServiceJobScheduler.class)
          .asEagerSingleton();
    }

    bind(ArtifactCollectionService.class)
        .annotatedWith(Names.named("AsyncArtifactCollectionService"))
        .to(ArtifactCollectionServiceAsyncImpl.class);
    bind(ArtifactCollectionService.class)
        .annotatedWith(Names.named("ArtifactCollectionService"))
        .to(ArtifactCollectionServiceImpl.class);
    bind(ArtifactCleanupService.class)
        .annotatedWith(Names.named("AsyncArtifactCleanupService"))
        .to(ArtifactCleanupServiceAsyncImpl.class);
    bind(CustomBuildSourceService.class).to(CustomBuildSourceServiceImpl.class);

    bind(ContainerSync.class).to(ContainerSyncImpl.class);
    bind(AwsLambdaService.class).to(AwsLambdaServiceImpl.class);
    bind(CloudToHarnessMappingService.class).to(CloudToHarnessMappingServiceImpl.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(HelmDeployService.class).to(HelmDeployServiceUnsupported.class).in(Singleton.class);
    bind(PcfDeploymentManager.class).to(PcfDeploymentManagerUnsupported.class).in(Singleton.class);

    bind(LogVerificationService.class).to(LogVerificationServiceImpl.class);
    bind(CVConfigurationService.class).to(CVConfigurationServiceImpl.class);
    bind(CVActivityLogService.class).to(CVActivityLogServiceImpl.class);
    bind(CvValidationService.class).to(CvValidationServiceImpl.class);
    bind(SubdomainUrlHelperIntfc.class).to(SubdomainUrlHelper.class);

    bind(LimitCheckerFactory.class).to(LimitCheckerFactoryImpl.class);
    bind(LimitConfigurationService.class).to(LimitConfigurationServiceMongo.class);
    bind(DefaultLimitsService.class).to(DefaultLimitsServiceImpl.class);
    bind(CounterService.class).to(CounterServiceImpl.class);

    bind(LimitVicinityHandler.class).to(LimitVicinityHandlerImpl.class);
    bind(PermitService.class).to(PermitServiceImpl.class);

    bind(GovernanceConfigService.class).to(GovernanceConfigServiceImpl.class);
    bind(MarketPlaceService.class).to(MarketPlaceServiceImpl.class);
    bind(AwsMarketPlaceApiHandler.class).to(AwsMarketPlaceApiHandlerImpl.class);
    bind(AlertVisibilityChecker.class).to(AlertVisibilityCheckerImpl.class);

    bind(ServiceNowService.class).to(ServiceNowServiceImpl.class);
    bind(PagerDutyService.class).to(PagerDutyServiceImpl.class);
    bind(ApprovalPolingService.class).to(ApprovalPolingServiceImpl.class);
    // Start of deployment trigger dependencies
    bind(TriggerExecutionService.class).to(TriggerExecutionServiceImpl.class);
    bind(FailedLoginAttemptCountChecker.class).to(FailedLoginAttemptCountCheckerImpl.class);

    bind(SegmentGroupEventJobService.class).to(SegmentGroupEventJobServiceImpl.class);

    // To support storing 'Files' in google cloud storage besides default Mongo GridFs.
    if (configuration.getFileStorageMode() == null) {
      // default to MONGO GridFs as file storage
      configuration.setFileStorageMode(DataStorageMode.MONGO);
    }

    bind(FileService.class).to(FileServiceImpl.class);
    bind(AlertNotificationRuleChecker.class).to(AlertNotificationRuleCheckerImpl.class);

    bind(new TypeLiteral<NotificationDispatcher<UserGroup>>() {})
        .annotatedWith(UseUserGroup.class)
        .to(UserGroupBasedDispatcher.class);

    bind(new TypeLiteral<NotificationDispatcher<NotificationGroup>>() {})
        .annotatedWith(UseNotificationGroup.class)
        .to(NotificationGroupBasedDispatcher.class);

    bind(PreDeploymentChecker.class).annotatedWith(RateLimitCheck.class).to(DeploymentRateLimitChecker.class);
    bind(PreDeploymentChecker.class).annotatedWith(ServiceInstanceUsage.class).to(SIUsageChecker.class);
    bind(PreDeploymentChecker.class).annotatedWith(AccountExpiryCheck.class).to(AccountExpirationChecker.class);

    bind(new TypeLiteral<GovernanceStatusEvaluator<Pipeline>>() {})
        .annotatedWith(OnPipeline.class)
        .to(PipelineStatusEvaluator.class);

    bind(new TypeLiteral<GovernanceStatusEvaluator<Workflow>>() {})
        .annotatedWith(OnWorkflow.class)
        .to(WorkflowStatusEvaluator.class);

    bind(PipelineGovernanceService.class).to(PipelineGovernanceServiceImpl.class);

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollectorExecutor"))
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("DeploymentReconTaskExecutor"))
        .toInstance(ThreadPool.create(1, 5, 10, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("DeploymentReconTaskExecutor-%d").build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("BuildSourceCallbackExecutor"))
        .toInstance(new ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(25000),
            new ThreadFactoryBuilder().setNameFormat("BuildSourceCallbackExecutor-%d").build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("BuildSourceCleanupCallbackExecutor"))
        .toInstance(new ThreadPoolExecutor(2, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(25000),
            new ThreadFactoryBuilder().setNameFormat("BuildSourceCleanupCallbackExecutor-%d").build()));

    bind(DashboardSettingsService.class).to(DashboardSettingsServiceImpl.class);
    bind(NameService.class).to(NameServiceImpl.class);
    // bind(TimeScaleDBService.class).toInstance(new TimeScaleDBServiceImpl(configuration.getTimeScaleDBConfig()));
    try {
      bind(TimeScaleDBService.class)
          .toConstructor(TimeScaleDBServiceImpl.class.getConstructor(TimeScaleDBConfig.class));
    } catch (NoSuchMethodException e) {
      log.error("TimeScaleDbServiceImpl Initialization Failed in due to missing constructor", e);
    }
    bind(TimeScaleDBConfig.class)
        .annotatedWith(Names.named("TimeScaleDBConfig"))
        .toInstance(configuration.getTimeScaleDBConfig() != null ? configuration.getTimeScaleDBConfig()
                                                                 : TimeScaleDBConfig.builder().build());
    bind(BigQueryService.class).to(BigQueryServiceImpl.class);
    if (configuration.getExecutionLogsStorageMode() == null) {
      configuration.setExecutionLogsStorageMode(DataStorageMode.MONGO);
    }
    bind(DeploymentReconService.class).to(DeploymentReconServiceImpl.class);
    bindFeatures();
    bindGcpMarketplaceProductHandlers();

    bind(FeatureService.class).to(FeatureServiceImpl.class);
    bind(ServerlessInstanceService.class).to(ServerlessInstanceServiceImpl.class);
    bind(ServerlessInstanceStatService.class).to(ServerlessInstanceStatServiceImpl.class);
    bind(ServerlessDashboardService.class).to(ServerlessDashboardServiceImpl.class);
    bind(ScalyrService.class).to(ScalyrServiceImpl.class);
    bind(ApmVerificationService.class).to(ApmVerificationServiceImpl.class);
    bind(CustomDeploymentTypeService.class).to(CustomDeploymentTypeServiceImpl.class);
    bind(NGGitService.class).to(NGGitServiceImpl.class);
    bind(GitClientV2.class).to(GitClientV2Impl.class);

    bind(AnomalyService.class).to(AnomalyServiceImpl.class);

    ApiBlocker apiBlocker = new ApiBlocker();
    requestInjection(apiBlocker);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(RestrictedApi.class), apiBlocker);

    switch (configuration.getExecutionLogsStorageMode()) {
      case GOOGLE_CLOUD_DATA_STORE:
        bind(DataStoreService.class).to(GoogleDataStoreServiceImpl.class);
        break;
      case MONGO:
        bind(DataStoreService.class).to(MongoDataStoreServiceImpl.class);
        break;
      default:
        throw new InvalidArgumentsException(
            Pair.of(configuration.getExecutionLogsStorageMode().toString(), "Invalid execution log data storage mode"));
    }

    // End of deployment trigger dependencies

    install(new PerpetualTaskServiceModule());
    install(new CESetupServiceModule());
    install(new CVNextGenCommonsServiceModule());
    try {
      install(new ConnectorResourceClientModule(configuration.getNgManagerServiceHttpClientConfig(),
          configuration.getPortal().getJwtNextGenManagerSecret(), MANAGER.getServiceId()));
      install(new OrganizationManagementClientModule(configuration.getNgManagerServiceHttpClientConfig(),
          configuration.getPortal().getJwtNextGenManagerSecret(), MANAGER.getServiceId()));
    } catch (Exception ex) {
      log.info("Could not create the connector resource client module", ex);
    }

    // Orchestration Dependencies
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    bind(CustomExecutionService.class).to(CustomExecutionServiceImpl.class);
    MapBinder<TaskCategory, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), TaskCategory.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskCategory.DELEGATE_TASK_V1).to(DelegateTaskExecutor.class);

    bind(CVDataCollectionTaskService.class).to(CVDataCollectionTaskServiceImpl.class);
    bind(HelmChartService.class).to(HelmChartServiceImpl.class);
    bind(LogStreamingServiceRestClient.class).toProvider(LogStreamingServiceClientFactory.class);
    bind(IInstanceReconService.class).to(InstanceReconServiceImpl.class);
  }

  private void bindFeatures() {
    MapBinder<String, Feature> mapBinder = MapBinder.newMapBinder(binder(), String.class, Feature.class);

    mapBinder.addBinding(IpWhitelistingFeature.FEATURE_NAME).to(IpWhitelistingFeature.class).in(Singleton.class);
    mapBinder.addBinding(GovernanceFeature.FEATURE_NAME).to(GovernanceFeature.class).in(Singleton.class);
    mapBinder.addBinding(UsersFeature.FEATURE_NAME).to(UsersFeature.class).in(Singleton.class);
    mapBinder.addBinding(DelegatesFeature.FEATURE_NAME).to(DelegatesFeature.class).in(Singleton.class);
    mapBinder.addBinding(TemplateLibraryFeature.FEATURE_NAME).to(TemplateLibraryFeature.class).in(Singleton.class);
    mapBinder.addBinding(FlowControlFeature.FEATURE_NAME).to(FlowControlFeature.class).in(Singleton.class);
    mapBinder.addBinding(ApprovalFlowFeature.FEATURE_NAME).to(ApprovalFlowFeature.class).in(Singleton.class);
    mapBinder.addBinding(ServiceNowNotificationFeature.FEATURE_NAME)
        .to(ServiceNowNotificationFeature.class)
        .in(Singleton.class);
    mapBinder.addBinding(JiraNotificationFeature.FEATURE_NAME).to(JiraNotificationFeature.class).in(Singleton.class);
    mapBinder.addBinding(PagerDutyNotificationFeature.FEATURE_NAME)
        .to(PagerDutyNotificationFeature.class)
        .in(Singleton.class);
    mapBinder.addBinding(ApiKeysFeature.FEATURE_NAME).to(ApiKeysFeature.class).in(Singleton.class);
    mapBinder.addBinding(PipelineGovernanceFeature.FEATURE_NAME)
        .to(PipelineGovernanceFeature.class)
        .in(Singleton.class);
    mapBinder.addBinding(TwoFactorAuthenticationFeature.FEATURE_NAME)
        .to(TwoFactorAuthenticationFeature.class)
        .in(Singleton.class);
    mapBinder.addBinding(SamlFeature.FEATURE_NAME).to(SamlFeature.class).in(Singleton.class);
    mapBinder.addBinding(LdapFeature.FEATURE_NAME).to(LdapFeature.class).in(Singleton.class);
    mapBinder.addBinding(GitOpsFeature.FEATURE_NAME).to(GitOpsFeature.class).in(Singleton.class);
    mapBinder.addBinding(RbacFeature.FEATURE_NAME).to(RbacFeature.class).in(Singleton.class);
    mapBinder.addBinding(SecretsManagementFeature.FEATURE_NAME).to(SecretsManagementFeature.class).in(Singleton.class);
    mapBinder.addBinding(AuditTrailFeature.FEATURE_NAME).to(AuditTrailFeature.class).in(Singleton.class);
    mapBinder.addBinding(RestApiFeature.FEATURE_NAME).to(RestApiFeature.class).in(Singleton.class);
    mapBinder.addBinding(DeploymentHistoryFeature.FEATURE_NAME).to(DeploymentHistoryFeature.class).in(Singleton.class);
    mapBinder.addBinding(TagsFeature.FEATURE_NAME).to(TagsFeature.class).in(Singleton.class);
    mapBinder.addBinding(CustomDashboardFeature.FEATURE_NAME).to(CustomDashboardFeature.class).in(Singleton.class);

    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(IpWhitelistingFeature.FEATURE_NAME))
        .to(IpWhitelistingFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(GovernanceFeature.FEATURE_NAME))
        .to(GovernanceFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(UsersFeature.FEATURE_NAME))
        .to(UsersFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(DelegatesFeature.FEATURE_NAME))
        .to(DelegatesFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(TemplateLibraryFeature.FEATURE_NAME))
        .to(TemplateLibraryFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(FlowControlFeature.FEATURE_NAME))
        .to(FlowControlFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(ApprovalFlowFeature.FEATURE_NAME))
        .to(ApprovalFlowFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(ServiceNowNotificationFeature.FEATURE_NAME))
        .to(ServiceNowNotificationFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(JiraNotificationFeature.FEATURE_NAME))
        .to(JiraNotificationFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(PagerDutyNotificationFeature.FEATURE_NAME))
        .to(PagerDutyNotificationFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(ApiKeysFeature.FEATURE_NAME))
        .to(ApiKeysFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(PipelineGovernanceFeature.FEATURE_NAME))
        .to(PipelineGovernanceFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(TwoFactorAuthenticationFeature.FEATURE_NAME))
        .to(TwoFactorAuthenticationFeature.class);
    binder().bind(PremiumFeature.class).annotatedWith(Names.named(SamlFeature.FEATURE_NAME)).to(SamlFeature.class);
    binder().bind(PremiumFeature.class).annotatedWith(Names.named(LdapFeature.FEATURE_NAME)).to(LdapFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(SecretsManagementFeature.FEATURE_NAME))
        .to(SecretsManagementFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(GitOpsFeature.FEATURE_NAME))
        .to(GitOpsFeature.class);
    binder().bind(UsageLimitedFeature.class).annotatedWith(Names.named(RbacFeature.FEATURE_NAME)).to(RbacFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(AuditTrailFeature.FEATURE_NAME))
        .to(AuditTrailFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(RestApiFeature.FEATURE_NAME))
        .to(RestApiFeature.class);
    binder()
        .bind(RestrictedFeature.class)
        .annotatedWith(Names.named(DeploymentHistoryFeature.FEATURE_NAME))
        .to(DeploymentHistoryFeature.class);
    binder().bind(PremiumFeature.class).annotatedWith(Names.named(TagsFeature.FEATURE_NAME)).to(TagsFeature.class);
    binder()
        .bind(PremiumFeature.class)
        .annotatedWith(Names.named(CustomDashboardFeature.FEATURE_NAME))
        .to(CustomDashboardFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(CeBudgetFeature.FEATURE_NAME))
        .to(CeBudgetFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(CeCloudAccountFeature.FEATURE_NAME))
        .to(CeCloudAccountFeature.class);
    binder()
        .bind(UsageLimitedFeature.class)
        .annotatedWith(Names.named(CeClusterFeature.FEATURE_NAME))
        .to(CeClusterFeature.class);
  }

  void registerSecretManagementBindings() {
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(ManagerDecryptionService.class).to(ManagerDecryptionServiceImpl.class);
    bind(RuntimeCredentialsInjector.class).annotatedWith(Names.named(hashicorpvault)).to(VaultServiceImpl.class);
    bind(SecretManager.class).to(SecretManagerImpl.class);
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(NGSecretFileService.class).to(NGSecretFileServiceImpl.class);
    bind(SecretsDelegateCacheService.class).to(SecretsDelegateCacheServiceImpl.class);
    bind(SecretManagerConfigService.class).to(SecretManagerConfigServiceImpl.class);
    bind(VaultService.class).to(VaultServiceImpl.class);
    bind(AwsSecretsManagerService.class).to(AwsSecretsManagerServiceImpl.class);
    bind(KmsService.class).to(KmsServiceImpl.class);
    bind(CyberArkService.class).to(CyberArkServiceImpl.class);
    bind(GcpSecretsManagerService.class).to(GcpSecretsManagerServiceImpl.class);
    bind(GcpSecretsManagerServiceV2.class).to(GcpSecretsManagerServiceV2Impl.class);
    bind(AzureSecretsManagerService.class).to(AzureSecretsManagerServiceImpl.class);
    bind(LocalSecretManagerService.class).to(LocalSecretManagerServiceImpl.class);
    bind(CustomSecretsManagerService.class).to(CustomSecretsManagerServiceImpl.class);
    bind(CustomEncryptedDataDetailBuilder.class).to(CustomEncryptedDataDetailBuilderImpl.class);
    bind(SecretsFileService.class).to(SecretsFileServiceImpl.class);
    bind(SecretsAuditService.class).to(SecretsAuditServiceImpl.class);
    bind(SecretsRBACService.class).to(SecretsRBACServiceImpl.class);
    bind(SecretsManagerRBACService.class).to(SecretsManagerRBACServiceImpl.class);

    binder()
        .bind(VaultEncryptor.class)
        .annotatedWith(Names.named(Encryptors.HASHICORP_VAULT_ENCRYPTOR.getName()))
        .to(ManagerVaultEncryptor.class);

    binder()
        .bind(VaultEncryptor.class)
        .annotatedWith(Names.named(Encryptors.AWS_VAULT_ENCRYPTOR.getName()))
        .to(ManagerVaultEncryptor.class);

    binder()
        .bind(VaultEncryptor.class)
        .annotatedWith(Names.named(Encryptors.AZURE_VAULT_ENCRYPTOR.getName()))
        .to(ManagerVaultEncryptor.class);

    binder()
        .bind(VaultEncryptor.class)
        .annotatedWith(Names.named(Encryptors.GCP_VAULT_ENCRYPTOR.getName()))
        .to(ManagerVaultEncryptor.class);

    binder()
        .bind(VaultEncryptor.class)
        .annotatedWith(Names.named(Encryptors.CYBERARK_VAULT_ENCRYPTOR.getName()))
        .to(ManagerVaultEncryptor.class);

    binder()
        .bind(KmsEncryptor.class)
        .annotatedWith(Names.named(Encryptors.AWS_KMS_ENCRYPTOR.getName()))
        .to(ManagerKmsEncryptor.class);

    binder()
        .bind(KmsEncryptor.class)
        .annotatedWith(Names.named(Encryptors.GCP_KMS_ENCRYPTOR.getName()))
        .to(ManagerKmsEncryptor.class);

    binder()
        .bind(KmsEncryptor.class)
        .annotatedWith(Names.named(Encryptors.LOCAL_ENCRYPTOR.getName()))
        .to(LocalEncryptor.class);

    binder()
        .bind(KmsEncryptor.class)
        .annotatedWith(Names.named(Encryptors.GLOBAL_AWS_KMS_ENCRYPTOR.getName()))
        .to(AwsKmsEncryptor.class);

    binder()
        .bind(KmsEncryptor.class)
        .annotatedWith(Names.named(Encryptors.GLOBAL_GCP_KMS_ENCRYPTOR.getName()))
        .to(GcpKmsEncryptor.class);

    binder()
        .bind(CustomEncryptor.class)
        .annotatedWith(Names.named(Encryptors.CUSTOM_ENCRYPTOR.getName()))
        .to(ManagerCustomEncryptor.class);

    binder()
        .bind(SecretSetupUsageBuilder.class)
        .annotatedWith(Names.named(SecretSetupUsageBuilders.SERVICE_VARIABLE_SETUP_USAGE_BUILDER.getName()))
        .to(ServiceVariableSetupUsageBuilder.class);
    binder()
        .bind(SecretSetupUsageBuilder.class)
        .annotatedWith(Names.named(SecretSetupUsageBuilders.CONFIG_FILE_SETUP_USAGE_BUILDER.getName()))
        .to(ConfigFileSetupUsageBuilder.class);
    binder()
        .bind(SecretSetupUsageBuilder.class)
        .annotatedWith(Names.named(SecretSetupUsageBuilders.SETTING_ATTRIBUTE_SETUP_USAGE_BUILDER.getName()))
        .to(SettingAttributeSetupUsageBuilder.class);
    binder()
        .bind(SecretSetupUsageBuilder.class)
        .annotatedWith(Names.named(SecretSetupUsageBuilders.SECRET_MANAGER_CONFIG_SETUP_USAGE_BUILDER.getName()))
        .to(SecretManagerSetupUsageBuilder.class);
  }

  private void bindGcpMarketplaceProductHandlers() {
    MapBinder<String, GcpProductHandler> binder =
        MapBinder.newMapBinder(binder(), String.class, GcpProductHandler.class);

    binder.addBinding("harness-continuous-delivery").to(CDProductHandler.class).in(Singleton.class);
  }

  @Provides
  CfClient provideCfClient() {
    CfClientConfig cfClientConfig = configuration.getCfClientConfig();
    if (cfClientConfig == null) {
      return null;
    }
    log.info("Using CF API key {}", cfClientConfig.getApiKey());
    String apiKey = cfClientConfig.getApiKey();

    try {
      return new CfClient(apiKey);
    } catch (CfClientException | ApiException | CFException e) {
      log.error("Failed to initialize the CF client, {}", e.getMessage());
    }
    return null;
  }

  @Provides
  @Singleton
  public FeatureRestrictions featureRestrictions() throws IOException {
    String featureRestrictions =
        IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("feature-restrictions.yml"),
            StandardCharsets.UTF_8);

    return new YamlUtils().read(featureRestrictions, FeatureRestrictions.class);
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return Collections.singletonList(getPersistentLockerCloseable(injector));
  }

  @NotNull
  private Closeable getPersistentLockerCloseable(Injector injector) {
    return () -> {
      PersistentLocker persistentLocker = injector.getInstance(PersistentLocker.class);
      if (persistentLocker instanceof Managed) {
        try {
          ((Managed) persistentLocker).stop();
        } catch (Exception e) {
          throw new IOException(e);
        }
      }
    };
  }
}
