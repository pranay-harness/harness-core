package software.wings.app;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import io.harness.OrchestrationModule;
import io.harness.event.lite.FeatureAvailabilityProviderService;
import io.harness.event.lite.FeatureAvailabilityProviderServiceImpl;
import io.harness.exception.WingsException;
import io.harness.govern.DependencyModule;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitCheckerFactoryImpl;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.configuration.LimitConfigurationServiceMongo;
import io.harness.limits.counter.service.CounterService;
import io.harness.limits.counter.service.CounterServiceImpl;
import io.harness.limits.defaults.service.DefaultLimitsService;
import io.harness.limits.defaults.service.DefaultLimitsServiceImpl;
import io.harness.notifications.AlertNotificationRuleChecker;
import io.harness.notifications.AlertNotificationRuleCheckerImpl;
import io.harness.notifications.AlertVisibilityChecker;
import io.harness.notifications.AlertVisibilityCheckerImpl;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.SchedulerConfig;
import io.harness.threading.ThreadPool;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import software.wings.DataStorageMode;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.NotificationGroup;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.security.UserGroup;
import software.wings.beans.trigger.Condition;
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
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.dl.exportimport.WingsMongoExportImport;
import software.wings.helpers.ext.ami.AmiService;
import software.wings.helpers.ext.ami.AmiServiceImpl;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.helpers.ext.azure.AcrServiceImpl;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrClassicServiceImpl;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.ecr.EcrServiceImpl;
import software.wings.helpers.ext.gcr.GcrService;
import software.wings.helpers.ext.gcr.GcrServiceImpl;
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
import software.wings.licensing.DatabaseLicenseProviderImpl;
import software.wings.licensing.LicenseProvider;
import software.wings.licensing.LicenseService;
import software.wings.licensing.LicenseServiceImpl;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.scheduler.ServiceJobScheduler;
import software.wings.security.saml.SamlUserGroupSync;
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
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.impl.AssignDelegateServiceImpl;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.impl.AwsHelperResourceServiceImpl;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.AzureInfrastructureProvider;
import software.wings.service.impl.AzureResourceServiceImpl;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.BarrierServiceImpl;
import software.wings.service.impl.BuildSourceServiceImpl;
import software.wings.service.impl.CatalogServiceImpl;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.CommandServiceImpl;
import software.wings.service.impl.ConfigServiceImpl;
import software.wings.service.impl.DelegateProfileServiceImpl;
import software.wings.service.impl.DelegateScopeServiceImpl;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.DownloadTokenServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.EmailNotificationServiceImpl;
import software.wings.service.impl.EntityVersionServiceImpl;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.ExternalApiRateLimitingServiceImpl;
import software.wings.service.impl.FeatureFlagServiceImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.GcpInfrastructureProvider;
import software.wings.service.impl.GcrBuildServiceImpl;
import software.wings.service.impl.GcsBuildServiceImpl;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.HarnessApiKeyServiceImpl;
import software.wings.service.impl.HarnessSampleAppServiceImpl;
import software.wings.service.impl.HarnessUserGroupServiceImpl;
import software.wings.service.impl.HostServiceImpl;
import software.wings.service.impl.InfrastructureMappingServiceImpl;
import software.wings.service.impl.InfrastructureProvisionerServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.impl.MarketPlaceServiceImpl;
import software.wings.service.impl.MigrationServiceImpl;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.NotificationDispatcherServiceImpl;
import software.wings.service.impl.NotificationServiceImpl;
import software.wings.service.impl.NotificationSetupServiceImpl;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.impl.PipelineServiceImpl;
import software.wings.service.impl.PluginServiceImpl;
import software.wings.service.impl.PreferenceServiceImpl;
import software.wings.service.impl.ResourceConstraintServiceImpl;
import software.wings.service.impl.RoleServiceImpl;
import software.wings.service.impl.SSOServiceImpl;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.impl.ServiceInstanceServiceImpl;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.impl.ServiceVariableServiceImpl;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.SftpBuildServiceImpl;
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
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.LearningEngineAnalysisServiceImpl;
import software.wings.service.impl.analysis.LogLabelingServiceImpl;
import software.wings.service.impl.analysis.MetricDataAnalysisServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsServiceImpl;
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
import software.wings.service.impl.compliance.GovernanceConfigServiceImpl;
import software.wings.service.impl.dynatrace.DynaTraceServiceImpl;
import software.wings.service.impl.elk.ElkAnalysisServiceImpl;
import software.wings.service.impl.expression.ExpressionBuilderServiceImpl;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.impl.infra.InfraDownloadServiceImpl;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl;
import software.wings.service.impl.instance.DeploymentServiceImpl;
import software.wings.service.impl.instance.InstanceServiceImpl;
import software.wings.service.impl.instance.licensing.InstanceLimitProviderImpl;
import software.wings.service.impl.instance.licensing.InstanceUsageLimitCheckerImpl;
import software.wings.service.impl.instance.licensing.InstanceUsageLimitExcessHandlerImpl;
import software.wings.service.impl.instance.limits.LimitVicinityHandlerImpl;
import software.wings.service.impl.instance.stats.InstanceStatServiceImpl;
import software.wings.service.impl.instance.stats.collector.StatsCollectorImpl;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.ContainerSyncImpl;
import software.wings.service.impl.newrelic.NewRelicServiceImpl;
import software.wings.service.impl.notifications.NotificationDispatcher;
import software.wings.service.impl.notifications.NotificationGroupBasedDispatcher;
import software.wings.service.impl.notifications.UseNotificationGroup;
import software.wings.service.impl.notifications.UseUserGroup;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.impl.personalization.PersonalizationServiceImpl;
import software.wings.service.impl.prometheus.PrometheusAnalysisServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.KmsServiceImpl;
import software.wings.service.impl.security.ManagerDecryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.security.SecretManagerImpl;
import software.wings.service.impl.security.VaultServiceImpl;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl;
import software.wings.service.impl.splunk.SplunkAnalysisServiceImpl;
import software.wings.service.impl.stackdriver.StackDriverServiceImpl;
import software.wings.service.impl.sumo.SumoLogicAnalysisServiceImpl;
import software.wings.service.impl.trigger.ArtifactTriggerProcessor;
import software.wings.service.impl.trigger.DeploymentTriggerServiceImpl;
import software.wings.service.impl.trigger.TriggerExecutionServiceImpl;
import software.wings.service.impl.trigger.TriggerProcessor;
import software.wings.service.impl.trigger.TriggerServiceImpl;
import software.wings.service.impl.verification.CV24x7DashboardServiceImpl;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
import software.wings.service.impl.verification.CvValidationService;
import software.wings.service.impl.verification.CvValidationServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.impl.yaml.AppYamlResourceServiceImpl;
import software.wings.service.impl.yaml.EntityUpdateServiceImpl;
import software.wings.service.impl.yaml.GitClientUnsupported;
import software.wings.service.impl.yaml.K8sGlobalConfigServiceUnsupported;
import software.wings.service.impl.yaml.YamlArtifactStreamServiceImpl;
import software.wings.service.impl.yaml.YamlChangeSetServiceImpl;
import software.wings.service.impl.yaml.YamlDirectoryServiceImpl;
import software.wings.service.impl.yaml.YamlGitServiceImpl;
import software.wings.service.impl.yaml.YamlHistoryServiceImpl;
import software.wings.service.impl.yaml.YamlResourceServiceImpl;
import software.wings.service.impl.yaml.service.YamlCloneServiceImpl;
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
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.AwsHelperResourceService;
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
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.EcrClassicBuildService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ExternalApiRateLimitingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.GcsBuildService;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.service.intfc.HarnessSampleAppService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.MarketPlaceService;
import software.wings.service.intfc.MetricDataAnalysisService;
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
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SftpBuildService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.service.intfc.SmbBuildService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WebHookService;
import software.wings.service.intfc.WhitelistService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogLabelingService;
import software.wings.service.intfc.analysis.LogVerificationService;
import software.wings.service.intfc.analysis.LogVerificationServiceImpl;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
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
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.expression.ExpressionBuilderService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.limits.LimitVicinityHandler;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.service.intfc.servicenow.ServiceNowService;
import software.wings.service.intfc.splunk.SplunkAnalysisService;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.trigger.TriggerExecutionService;
import software.wings.service.intfc.verification.CV24x7DashboardService;
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
import software.wings.service.intfc.yaml.clone.YamlCloneService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExpressionProcessorFactory;
import software.wings.utils.HostValidationService;
import software.wings.utils.HostValidationServiceImpl;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Guice Module for initializing all beans.
 *
 * @author Rishi
 */
public class WingsModule extends DependencyModule {
  private MainConfiguration configuration;

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration Dropwizard configuration
   */
  public WingsModule(MainConfiguration configuration) {
    this.configuration = configuration;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(AuthService.class).to(AuthServiceImpl.class);
    bind(MainConfiguration.class).toInstance(configuration);
    bind(SchedulerConfig.class)
        .annotatedWith(Names.named("BackgroundSchedule"))
        .toInstance(configuration.getBackgroundSchedulerConfig());
    bind(QueueController.class).to(ConfigurationController.class);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(AppService.class).to(AppServiceImpl.class);
    bind(HarnessSampleAppService.class).to(HarnessSampleAppServiceImpl.class);
    bind(ApplicationManifestService.class).to(ApplicationManifestServiceImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(AuditService.class).to(AuditServiceImpl.class);
    bind(ArtifactStreamService.class).to(ArtifactStreamServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
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
    bind(BuildSourceService.class).to(BuildSourceServiceImpl.class);
    bind(ServiceVariableService.class).to(ServiceVariableServiceImpl.class);
    bind(AccountService.class).to(AccountServiceImpl.class);
    bind(PipelineService.class).to(PipelineServiceImpl.class);
    bind(NotificationSetupService.class).to(NotificationSetupServiceImpl.class);
    bind(NotificationDispatcherService.class).to(NotificationDispatcherServiceImpl.class);
    bind(ServiceClassLocator.class);
    bind(EntityVersionService.class).to(EntityVersionServiceImpl.class);
    bind(PluginService.class).to(PluginServiceImpl.class);
    bind(CommandService.class).to(CommandServiceImpl.class);
    bind(DelegateService.class).to(DelegateServiceImpl.class);
    bind(DelegateScopeService.class).to(DelegateScopeServiceImpl.class);
    bind(BambooService.class).to(BambooServiceImpl.class);
    bind(BambooBuildService.class).to(BambooBuildServiceImpl.class);
    bind(BarrierService.class).to(BarrierServiceImpl.class);
    bind(DownloadTokenService.class).to(DownloadTokenServiceImpl.class);
    bind(CloudWatchService.class).to(CloudWatchServiceImpl.class);
    bind(SlackNotificationService.class).to(SlackNotificationServiceImpl.class);
    bind(EcsContainerService.class).to(EcsContainerServiceImpl.class);
    bind(AwsClusterService.class).to(AwsClusterServiceImpl.class);
    bind(GkeClusterService.class).to(GkeClusterServiceImpl.class);
    bind(KubernetesContainerService.class).to(KubernetesContainerServiceImpl.class);
    bind(InfrastructureMappingService.class).to(InfrastructureMappingServiceImpl.class);
    bind(InfrastructureProvisionerService.class).to(InfrastructureProvisionerServiceImpl.class);
    bind(ResourceConstraintService.class).to(ResourceConstraintServiceImpl.class);
    bind(LicenseService.class).to(LicenseServiceImpl.class);
    bind(LicenseProvider.class).to(DatabaseLicenseProviderImpl.class);
    bind(AppdynamicsService.class).to(AppdynamicsServiceImpl.class);
    bind(PersonalizationService.class).to(PersonalizationServiceImpl.class);
    bind(StackDriverService.class).to(StackDriverServiceImpl.class);
    bind(NewRelicService.class).to(NewRelicServiceImpl.class);
    bind(DynaTraceService.class).to(DynaTraceServiceImpl.class);
    bind(MetricDataAnalysisService.class).to(MetricDataAnalysisServiceImpl.class);
    bind(AnalysisService.class).to(AnalysisServiceImpl.class);
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
    bind(GcrService.class).to(GcrServiceImpl.class);
    bind(GcrBuildService.class).to(GcrBuildServiceImpl.class);
    bind(AcrService.class).to(AcrServiceImpl.class);
    bind(AcrBuildService.class).to(AcrBuildServiceImpl.class);
    bind(AmiService.class).to(AmiServiceImpl.class);
    bind(AmiBuildService.class).to(AmiBuildServiceImpl.class);
    bind(AwsHelperResourceService.class).to(AwsHelperResourceServiceImpl.class);
    bind(AzureResourceService.class).to(AzureResourceServiceImpl.class);
    bind(AssignDelegateService.class).to(AssignDelegateServiceImpl.class);
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
    bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
    bind(KmsService.class).to(KmsServiceImpl.class);
    bind(AlertService.class).to(AlertServiceImpl.class).in(Singleton.class);
    bind(AlertNotificationRuleService.class).to(AlertNotificationRuleServiceImpl.class);
    bind(YamlChangeSetService.class).to(YamlChangeSetServiceImpl.class);
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(ManagerDecryptionService.class).to(ManagerDecryptionServiceImpl.class);
    bind(SecretManagementDelegateService.class).to(SecretManagementDelegateServiceImpl.class);
    bind(SecretManager.class).to(SecretManagerImpl.class);
    bind(TriggerService.class).to(TriggerServiceImpl.class);
    bind(VaultService.class).to(VaultServiceImpl.class);
    bind(LearningEngineService.class).to(LearningEngineAnalysisServiceImpl.class);
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
    bind(DelegateProfileService.class).to(DelegateProfileServiceImpl.class);
    bind(AwsCFHelperServiceManager.class).to(AwsCFHelperServiceManagerImpl.class);
    bind(SumoLogicAnalysisService.class).to(SumoLogicAnalysisServiceImpl.class);
    bind(InstanceStatService.class).to(InstanceStatServiceImpl.class);
    bind(StatsCollector.class).to(StatsCollectorImpl.class);
    bind(LogLabelingService.class).to(LogLabelingServiceImpl.class);
    bind(AwsRoute53HelperServiceManager.class).to(AwsRoute53HelperServiceManagerImpl.class);
    bind(HarnessApiKeyService.class).to(HarnessApiKeyServiceImpl.class);
    bind(K8sGlobalConfigService.class).to(K8sGlobalConfigServiceUnsupported.class);

    bind(SamlUserGroupSync.class);
    bind(SmbService.class).to(SmbServiceImpl.class);
    bind(SmbBuildService.class).to(SmbBuildServiceImpl.class);
    bind(SftpService.class).to(SftpServiceImpl.class);
    bind(SftpBuildService.class).to(SftpBuildServiceImpl.class);

    bind(WingsMongoExportImport.class);

    bind(InstanceUsageLimitChecker.class).to(InstanceUsageLimitCheckerImpl.class);
    bind(InstanceUsageLimitExcessHandler.class).to(InstanceUsageLimitExcessHandlerImpl.class);
    bind(InstanceLimitProvider.class).to(InstanceLimitProviderImpl.class);

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

    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));

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
    bind(SSOSettingService.class).to(SSOSettingServiceImpl.class);
    bind(SSOService.class).to(SSOServiceImpl.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(HelmDeployService.class).to(HelmDeployServiceUnsupported.class).in(Singleton.class);
    bind(PcfDeploymentManager.class).to(PcfDeploymentManagerUnsupported.class).in(Singleton.class);

    bind(LogVerificationService.class).to(LogVerificationServiceImpl.class);
    bind(CVConfigurationService.class).to(CVConfigurationServiceImpl.class);
    bind(CvValidationService.class).to(CvValidationServiceImpl.class);

    bind(LimitCheckerFactory.class).to(LimitCheckerFactoryImpl.class);
    bind(LimitConfigurationService.class).to(LimitConfigurationServiceMongo.class);
    bind(DefaultLimitsService.class).to(DefaultLimitsServiceImpl.class);
    bind(CounterService.class).to(CounterServiceImpl.class);

    bind(LimitVicinityHandler.class).to(LimitVicinityHandlerImpl.class);
    bind(PermitService.class).to(PermitServiceImpl.class);

    bind(GovernanceConfigService.class).to(GovernanceConfigServiceImpl.class);
    bind(MarketPlaceService.class).to(MarketPlaceServiceImpl.class);
    bind(AlertVisibilityChecker.class).to(AlertVisibilityCheckerImpl.class);

    bind(FeatureAvailabilityProviderService.class).to(FeatureAvailabilityProviderServiceImpl.class);

    bind(ServiceNowService.class).to(ServiceNowServiceImpl.class);
    // Start of deployment trigger dependencies
    bind(DeploymentTriggerService.class).to(DeploymentTriggerServiceImpl.class);
    bind(TriggerExecutionService.class).to(TriggerExecutionServiceImpl.class);
    MapBinder<String, TriggerProcessor> triggerProcessorMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TriggerProcessor.class);

    triggerProcessorMapBinder.addBinding(Condition.Type.NEW_ARTIFACT.name()).to(ArtifactTriggerProcessor.class);

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

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollector"))
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    if (configuration.getExecutionLogsStorageMode() == null) {
      configuration.setExecutionLogsStorageMode(DataStorageMode.MONGO);
    }

    switch (configuration.getExecutionLogsStorageMode()) {
      case GOOGLE_CLOUD_DATA_STORE:
        bind(DataStoreService.class).to(GoogleDataStoreServiceImpl.class);
        break;
      case MONGO:
        bind(DataStoreService.class).to(MongoDataStoreServiceImpl.class);
        break;
      default:
        throw new WingsException(
            "Invalid execution log data storage mode: " + configuration.getExecutionLogsStorageMode());
    }

    // End of deployment trigger dependencies
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(
        TimeModule.getInstance(), VersionModule.getInstance(), OrchestrationModule.getInstance());
  }
}
