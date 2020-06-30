package io.harness.cvng;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.client.VerificationManagerServiceImpl;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.cvng.core.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.impl.AppDynamicsCVConfigTransformer;
import io.harness.cvng.core.services.impl.AppDynamicsDataCollectionInfoMapper;
import io.harness.cvng.core.services.impl.CVConfigServiceImpl;
import io.harness.cvng.core.services.impl.DSConfigServiceImpl;
import io.harness.cvng.core.services.impl.DataCollectionTaskServiceImpl;
import io.harness.cvng.core.services.impl.FeatureFlagServiceImpl;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;
import io.harness.cvng.core.services.impl.SplunkCVConfigTransformer;
import io.harness.cvng.core.services.impl.SplunkServiceImpl;
import io.harness.cvng.core.services.impl.TimeSeriesAnalysisServiceImpl;
import io.harness.cvng.core.services.impl.TimeSeriesServiceImpl;
import io.harness.cvng.statemachine.services.AnalysisStateMachineServiceImpl;
import io.harness.cvng.statemachine.services.OrchestrationServiceImpl;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
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
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
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
      bind(DataCollectionTaskService.class).to(DataCollectionTaskServiceImpl.class);
      bind(VerificationManagerService.class).to(VerificationManagerServiceImpl.class);
      bind(Clock.class).toInstance(Clock.systemUTC());
      bind(DSConfigService.class).to(DSConfigServiceImpl.class);
      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(DSConfigService.class).to(DSConfigServiceImpl.class);
      bind(MetricPackService.class).to(MetricPackServiceImpl.class);
      bind(SplunkService.class).to(SplunkServiceImpl.class);
      bind(CVConfigService.class).to(CVConfigServiceImpl.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsCVConfigTransformer.class);
      bind(CVConfigTransformer.class)
          .annotatedWith(Names.named(DataSourceType.SPLUNK.name()))
          .to(SplunkCVConfigTransformer.class);
      bind(DataCollectionInfoMapper.class)
          .annotatedWith(Names.named(DataSourceType.APP_DYNAMICS.name()))
          .to(AppDynamicsDataCollectionInfoMapper.class);

    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
