package io.harness.cvng;

import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.impl.ActivityServiceImpl;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.CD10ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.activity.source.services.impl.ActivitySourceServiceImpl;
import io.harness.cvng.activity.source.services.impl.CD10ActivitySourceServiceImpl;
import io.harness.cvng.activity.source.services.impl.KubernetesActivitySourceServiceImpl;
import io.harness.cvng.alert.services.AlertRuleAnomalyService;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.services.impl.AlertRuleAnomalyServiceImpl;
import io.harness.cvng.alert.services.impl.AlertRuleServiceImpl;
import io.harness.cvng.analysis.services.api.AnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.api.HealthVerificationService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnomalousPatternsService;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
import io.harness.cvng.analysis.services.api.VerificationJobInstanceAnalysisService;
import io.harness.cvng.analysis.services.impl.AnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.DeploymentLogAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.DeploymentTimeSeriesAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.HealthVerificationServiceImpl;
import io.harness.cvng.analysis.services.impl.LearningEngineTaskServiceImpl;
import io.harness.cvng.analysis.services.impl.LogAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.LogClusterServiceImpl;
import io.harness.cvng.analysis.services.impl.TimeSeriesAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.TimeSeriesAnomalousPatternsServiceImpl;
import io.harness.cvng.analysis.services.impl.TrendAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.VerificationJobInstanceAnalysisServiceImpl;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.NextGenServiceImpl;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.client.VerificationManagerServiceImpl;
import io.harness.cvng.core.entities.AppDynamicsCVConfig.AppDynamicsCVConfigUpdatableEntity;
import io.harness.cvng.core.entities.CVConfig.CVConfigUpdatableEntity;
import io.harness.cvng.core.entities.NewRelicCVConfig.NewRelicCVConfigUpdatableEntity;
import io.harness.cvng.core.entities.SplunkCVConfig.SplunkCVConfigUpdatableEntity;
import io.harness.cvng.core.entities.StackdriverCVConfig.StackDriverCVConfigUpdatableEntity;
import io.harness.cvng.core.jobs.AccountChangeEventMessageProcessor;
import io.harness.cvng.core.jobs.ConnectorChangeEventMessageProcessor;
import io.harness.cvng.core.jobs.ConsumerMessageProcessor;
import io.harness.cvng.core.jobs.OrganizationChangeEventMessageProcessor;
import io.harness.cvng.core.jobs.ProjectChangeEventMessageProcessor;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DataSourceConnectivityChecker;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.MonitoringSourceImportStatusCreator;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.NewRelicService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.cvng.core.services.api.StackdriverService;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.core.services.impl.AppDynamicsCVConfigTransformer;
import io.harness.cvng.core.services.impl.AppDynamicsDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.AppDynamicsServiceImpl;
import io.harness.cvng.core.services.impl.CVConfigServiceImpl;
import io.harness.cvng.core.services.impl.CVEventServiceImpl;
import io.harness.cvng.core.services.impl.CVNGLogServiceImpl;
import io.harness.cvng.core.services.impl.CVSetupServiceImpl;
import io.harness.cvng.core.services.impl.DSConfigServiceImpl;
import io.harness.cvng.core.services.impl.DataCollectionTaskServiceImpl;
import io.harness.cvng.core.services.impl.DefaultDeleteEntityByHandler;
import io.harness.cvng.core.services.impl.DeletedCVConfigServiceImpl;
import io.harness.cvng.core.services.impl.HostRecordServiceImpl;
import io.harness.cvng.core.services.impl.LogRecordServiceImpl;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;
import io.harness.cvng.core.services.impl.MonitoringSourcePerpetualTaskServiceImpl;
import io.harness.cvng.core.services.impl.NewRelicCVConfigTransformer;
import io.harness.cvng.core.services.impl.NewRelicServiceImpl;
import io.harness.cvng.core.services.impl.OnboardingServiceImpl;
import io.harness.cvng.core.services.impl.SplunkCVConfigTransformer;
import io.harness.cvng.core.services.impl.SplunkDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.SplunkServiceImpl;
import io.harness.cvng.core.services.impl.StackdriverCVConfigTransformer;
import io.harness.cvng.core.services.impl.StackdriverDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.StackdriverServiceImpl;
import io.harness.cvng.core.services.impl.TimeSeriesRecordServiceImpl;
import io.harness.cvng.core.services.impl.VerificationTaskServiceImpl;
import io.harness.cvng.core.services.impl.WebhookServiceImpl;
import io.harness.cvng.dashboard.services.api.HealthVerificationHeatMapService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.cvng.dashboard.services.impl.HealthVerificationHeatMapServiceImpl;
import io.harness.cvng.dashboard.services.impl.HeatMapServiceImpl;
import io.harness.cvng.dashboard.services.impl.LogDashboardServiceImpl;
import io.harness.cvng.dashboard.services.impl.TimeSeriesDashboardServiceImpl;
import io.harness.cvng.migration.impl.CVNGMigrationServiceImpl;
import io.harness.cvng.migration.service.CVNGMigrationService;
import io.harness.cvng.statemachine.services.AnalysisStateMachineServiceImpl;
import io.harness.cvng.statemachine.services.OrchestrationServiceImpl;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob.BlueGreenVerificationUpdatableEntity;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob.CanaryVerificationUpdatableEntity;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob.HealthVerificationUpdatableEntity;
import io.harness.cvng.verificationjob.entities.TestVerificationJob.TestVerificationUpdatableEntity;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobUpdatableEntity;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.cvng.verificationjob.services.impl.VerificationJobInstanceServiceImpl;
import io.harness.cvng.verificationjob.services.impl.VerificationJobServiceImpl;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.ff.FeatureFlagModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.redis.RedisConfig;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * Guice Module for initializing all beans.
 *
 * @author Raghu
 */
@Slf4j
public class CVServiceModule extends AbstractModule {
  private VerificationConfiguration verificationConfiguration;

  public CVServiceModule(VerificationConfiguration verificationConfiguration) {
    this.verificationConfiguration = verificationConfiguration;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    install(FeatureFlagModule.getInstance());

    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-cv-nextgen-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .setUncaughtExceptionHandler((t, e) -> log.error("error while processing task", e))
                .build()));
    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(
          IOUtils.toString(getClass().getClassLoader().getResourceAsStream("main/resources-filtered/versionInfo.yaml"),
              StandardCharsets.UTF_8));
      bind(QueueController.class).toInstance(new QueueController() {
        @Override
        public boolean isPrimary() {
          return true;
        }

        @Override
        public boolean isNotPrimary() {
          return false;
        }
      });
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
      bind(HPersistence.class).to(MongoPersistence.class);
      bind(TimeSeriesRecordService.class).to(TimeSeriesRecordServiceImpl.class);
      bind(OrchestrationService.class).to(OrchestrationServiceImpl.class);
      bind(AnalysisStateMachineService.class).to(AnalysisStateMachineServiceImpl.class);
      bind(TimeSeriesAnalysisService.class).to(TimeSeriesAnalysisServiceImpl.class);
      bind(TrendAnalysisService.class).to(TrendAnalysisServiceImpl.class);
      bind(LearningEngineTaskService.class).to(LearningEngineTaskServiceImpl.class);
      bind(LogClusterService.class).to(LogClusterServiceImpl.class);
      bind(LogAnalysisService.class).to(LogAnalysisServiceImpl.class);
      bind(DataCollectionTaskService.class).to(DataCollectionTaskServiceImpl.class);
      bind(VerificationManagerService.class).to(VerificationManagerServiceImpl.class);
      bind(Clock.class).toInstance(Clock.systemUTC());
      bind(DSConfigService.class).to(DSConfigServiceImpl.class);
      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(HeatMapService.class).to(HeatMapServiceImpl.class);
      bind(DSConfigService.class).to(DSConfigServiceImpl.class);
      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(SplunkService.class).to(SplunkServiceImpl.class);
      bind(CVConfigService.class).to(CVConfigServiceImpl.class);
      bind(DeletedCVConfigService.class).to(DeletedCVConfigServiceImpl.class);
      bind(VerificationJobUpdatableEntity.class)
          .annotatedWith(Names.named(VerificationJobType.HEALTH.name()))
          .to(HealthVerificationUpdatableEntity.class);
      bind(VerificationJobUpdatableEntity.class)
          .annotatedWith(Names.named(VerificationJobType.TEST.name()))
          .to(TestVerificationUpdatableEntity.class);
      bind(VerificationJobUpdatableEntity.class)
          .annotatedWith(Names.named(VerificationJobType.BLUE_GREEN.name()))
          .to(BlueGreenVerificationUpdatableEntity.class);
      bind(VerificationJobUpdatableEntity.class)
          .annotatedWith(Names.named(VerificationJobType.CANARY.name()))
          .to(CanaryVerificationUpdatableEntity.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsCVConfigTransformer.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.NEW_RELIC.name()))
          .to(NewRelicCVConfigTransformer.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
          .to(SplunkCVConfigTransformer.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.STACKDRIVER.name()))
          .to(StackdriverCVConfigTransformer.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsDataCollectionInfoMapper.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
          .to(SplunkDataCollectionInfoMapper.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.STACKDRIVER.name()))
          .to(StackdriverDataCollectionInfoMapper.class);

      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(AppDynamicsService.class).to(AppDynamicsServiceImpl.class);
      bind(VerificationJobService.class).to(VerificationJobServiceImpl.class);
      bind(LogRecordService.class).to(LogRecordServiceImpl.class);
      bind(VerificationJobInstanceService.class).to(VerificationJobInstanceServiceImpl.class);
      bind(VerificationTaskService.class).to(VerificationTaskServiceImpl.class);
      bind(TimeSeriesDashboardService.class).to(TimeSeriesDashboardServiceImpl.class);
      bind(ActivityService.class).to(ActivityServiceImpl.class);
      bind(AlertRuleService.class).to(AlertRuleServiceImpl.class);
      bind(LogDashboardService.class).to(LogDashboardServiceImpl.class);
      bind(WebhookService.class).to(WebhookServiceImpl.class);
      bind(DeploymentTimeSeriesAnalysisService.class).to(DeploymentTimeSeriesAnalysisServiceImpl.class);
      bind(NextGenService.class).to(NextGenServiceImpl.class);
      bind(HostRecordService.class).to(HostRecordServiceImpl.class);
      bind(KubernetesActivitySourceService.class).to(KubernetesActivitySourceServiceImpl.class);
      bind(DeploymentLogAnalysisService.class).to(DeploymentLogAnalysisServiceImpl.class);
      bind(VerificationJobInstanceAnalysisService.class).to(VerificationJobInstanceAnalysisServiceImpl.class);
      bind(HealthVerificationService.class).to(HealthVerificationServiceImpl.class);
      bind(HealthVerificationHeatMapService.class).to(HealthVerificationHeatMapServiceImpl.class);
      bind(AnalysisService.class).to(AnalysisServiceImpl.class);
      bind(OnboardingService.class).to(OnboardingServiceImpl.class);
      bind(CVSetupService.class).to(CVSetupServiceImpl.class);
      bindTheMonitoringSourceImportStatusCreators();
      bind(CVNGMigrationService.class).to(CVNGMigrationServiceImpl.class).in(Singleton.class);
      bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
      bind(StackdriverService.class).to(StackdriverServiceImpl.class);
      bind(CVEventService.class).to(CVEventServiceImpl.class);
      bind(RedisConfig.class)
          .annotatedWith(Names.named("lock"))
          .toInstance(verificationConfiguration.getEventsFrameworkConfiguration().getRedisConfig());
      bind(ConsumerMessageProcessor.class)
          .annotatedWith(Names.named(EventsFrameworkMetadataConstants.PROJECT_ENTITY))
          .to(ProjectChangeEventMessageProcessor.class);
      bind(ConsumerMessageProcessor.class)
          .annotatedWith(Names.named(EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY))
          .to(OrganizationChangeEventMessageProcessor.class);
      bind(ConsumerMessageProcessor.class)
          .annotatedWith(Names.named(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY))
          .to(AccountChangeEventMessageProcessor.class);
      bind(ConsumerMessageProcessor.class)
          .annotatedWith(Names.named(EventsFrameworkMetadataConstants.CONNECTOR_ENTITY))
          .to(ConnectorChangeEventMessageProcessor.class);
      bind(AlertRuleAnomalyService.class).to(AlertRuleAnomalyServiceImpl.class);
      bind(NewRelicService.class).to(NewRelicServiceImpl.class);
      bind(String.class)
          .annotatedWith(Names.named("portalUrl"))
          .toInstance(verificationConfiguration.getPortalUrl().endsWith("/")
                  ? verificationConfiguration.getPortalUrl()
                  : verificationConfiguration.getPortalUrl() + "/");
      bind(CVNGLogService.class).to(CVNGLogServiceImpl.class);
      bind(ActivitySourceService.class).to(ActivitySourceServiceImpl.class);
      bind(DeleteEntityByHandler.class).to(DefaultDeleteEntityByHandler.class);
      bind(TimeSeriesAnomalousPatternsService.class).to(TimeSeriesAnomalousPatternsServiceImpl.class);
      bind(CD10ActivitySourceService.class).to(CD10ActivitySourceServiceImpl.class);

      bind(MonitoringSourcePerpetualTaskService.class).to(MonitoringSourcePerpetualTaskServiceImpl.class);

      MapBinder<DataSourceType, DataSourceConnectivityChecker> dataSourceTypeToServiceMapBinder =
          MapBinder.newMapBinder(binder(), DataSourceType.class, DataSourceConnectivityChecker.class);
      dataSourceTypeToServiceMapBinder.addBinding(DataSourceType.APP_DYNAMICS).to(AppDynamicsService.class);
      dataSourceTypeToServiceMapBinder.addBinding(DataSourceType.SPLUNK).to(SplunkService.class);
      dataSourceTypeToServiceMapBinder.addBinding(DataSourceType.STACKDRIVER).to(StackdriverService.class);
      dataSourceTypeToServiceMapBinder.addBinding(DataSourceType.KUBERNETES).to(KubernetesActivitySourceService.class);

      MapBinder<DataSourceType, CVConfigUpdatableEntity> dataSourceTypeCVConfigMapBinder =
          MapBinder.newMapBinder(binder(), DataSourceType.class, CVConfigUpdatableEntity.class);
      dataSourceTypeCVConfigMapBinder.addBinding(DataSourceType.APP_DYNAMICS)
          .to(AppDynamicsCVConfigUpdatableEntity.class);
      dataSourceTypeCVConfigMapBinder.addBinding(DataSourceType.NEW_RELIC).to(NewRelicCVConfigUpdatableEntity.class);
      dataSourceTypeCVConfigMapBinder.addBinding(DataSourceType.STACKDRIVER)
          .to(StackDriverCVConfigUpdatableEntity.class);
      dataSourceTypeCVConfigMapBinder.addBinding(DataSourceType.SPLUNK).to(SplunkCVConfigUpdatableEntity.class);

    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }

  private void bindTheMonitoringSourceImportStatusCreators() {
    bind(MonitoringSourceImportStatusCreator.class)
        .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
        .to(AppDynamicsService.class);
    bind(MonitoringSourceImportStatusCreator.class)
        .annotatedWith(Names.named(DataSourceType.STACKDRIVER.name()))
        .to(StackdriverService.class);
    bind(MonitoringSourceImportStatusCreator.class)
        .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
        .to(SplunkService.class);
    bind(MonitoringSourceImportStatusCreator.class)
        .annotatedWith(Names.named(DataSourceType.NEW_RELIC.name()))
        .to(NewRelicService.class);
  }

  @Provides
  @Singleton
  @Named("cvngParallelExecutor")
  public ExecutorService cvngParallelExecutor() {
    ExecutorService cvngParallelExecutor = ThreadPool.create(4, CVNextGenConstants.CVNG_MAX_PARALLEL_THREADS, 5,
        TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("cvngParallelExecutor-%d").setPriority(Thread.MIN_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> cvngParallelExecutor.shutdownNow()));
    return cvngParallelExecutor;
  }
}
