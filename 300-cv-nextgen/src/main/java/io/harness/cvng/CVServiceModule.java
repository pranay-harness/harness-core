package io.harness.cvng;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.impl.DeploymentLogAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.DeploymentTimeSeriesAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.LearningEngineTaskServiceImpl;
import io.harness.cvng.analysis.services.impl.LogAnalysisServiceImpl;
import io.harness.cvng.analysis.services.impl.LogClusterServiceImpl;
import io.harness.cvng.analysis.services.impl.TimeSeriesAnalysisServiceImpl;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.NextGenServiceImpl;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.client.VerificationManagerServiceImpl;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.core.services.impl.ActivityServiceImpl;
import io.harness.cvng.core.services.impl.AppDynamicsCVConfigTransformer;
import io.harness.cvng.core.services.impl.AppDynamicsDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.AppDynamicsServiceImpl;
import io.harness.cvng.core.services.impl.CVConfigServiceImpl;
import io.harness.cvng.core.services.impl.DSConfigServiceImpl;
import io.harness.cvng.core.services.impl.DataCollectionTaskServiceImpl;
import io.harness.cvng.core.services.impl.DeletedCVConfigServiceImpl;
import io.harness.cvng.core.services.impl.FeatureFlagServiceImpl;
import io.harness.cvng.core.services.impl.HostRecordServiceImpl;
import io.harness.cvng.core.services.impl.LogRecordServiceImpl;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;
import io.harness.cvng.core.services.impl.SplunkCVConfigTransformer;
import io.harness.cvng.core.services.impl.SplunkDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.SplunkServiceImpl;
import io.harness.cvng.core.services.impl.TimeSeriesServiceImpl;
import io.harness.cvng.core.services.impl.VerificationTaskServiceImpl;
import io.harness.cvng.core.services.impl.WebhookServiceImpl;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.impl.AnomalyServiceImpl;
import io.harness.cvng.dashboard.services.impl.HeatMapServiceImpl;
import io.harness.cvng.statemachine.services.AnalysisStateMachineServiceImpl;
import io.harness.cvng.statemachine.services.OrchestrationServiceImpl;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.cvng.verificationjob.services.impl.VerificationJobInstanceServiceImpl;
import io.harness.cvng.verificationjob.services.impl.VerificationJobServiceImpl;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Guice Module for initializing all beans.
 *
 * @author Raghu
 */
public class CVServiceModule extends AbstractModule {
  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-cv-nextgen-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
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
      bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
      bind(TimeSeriesService.class).to(TimeSeriesServiceImpl.class);
      bind(OrchestrationService.class).to(OrchestrationServiceImpl.class);
      bind(AnalysisStateMachineService.class).to(AnalysisStateMachineServiceImpl.class);
      bind(TimeSeriesAnalysisService.class).to(TimeSeriesAnalysisServiceImpl.class);
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
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsCVConfigTransformer.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
          .to(SplunkCVConfigTransformer.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsDataCollectionInfoMapper.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
          .to(SplunkDataCollectionInfoMapper.class);
      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(AppDynamicsService.class).to(AppDynamicsServiceImpl.class);
      bind(VerificationJobService.class).to(VerificationJobServiceImpl.class);
      bind(AnomalyService.class).to(AnomalyServiceImpl.class);
      bind(LogRecordService.class).to(LogRecordServiceImpl.class);
      bind(VerificationJobInstanceService.class).to(VerificationJobInstanceServiceImpl.class);
      bind(VerificationTaskService.class).to(VerificationTaskServiceImpl.class);
      bind(ActivityService.class).to(ActivityServiceImpl.class);
      bind(WebhookService.class).to(WebhookServiceImpl.class);
      bind(DeploymentTimeSeriesAnalysisService.class).to(DeploymentTimeSeriesAnalysisServiceImpl.class);
      bind(NextGenService.class).to(NextGenServiceImpl.class);
      bind(HostRecordService.class).to(HostRecordServiceImpl.class);
      bind(DeploymentLogAnalysisService.class).to(DeploymentLogAnalysisServiceImpl.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
