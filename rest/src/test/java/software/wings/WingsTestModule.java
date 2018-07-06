package software.wings;

import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import software.wings.common.thread.ThreadPool;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.artifactory.ArtifactoryServiceImpl;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.ArtifactoryBuildServiceImpl;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WingsTestModule extends AbstractModule {
  @Override
  protected void configure() {
    DelegateFileManager mockDelegateFileManager = mock(DelegateFileManager.class);
    bind(DelegateFileManager.class).toInstance(mockDelegateFileManager);
    bind(AmazonS3BuildService.class).to(AmazonS3BuildServiceImpl.class);
    bind(AmazonS3Service.class).to(AmazonS3ServiceImpl.class);
    bind(NewRelicDelegateService.class).to(NewRelicDelgateServiceImpl.class);
    bind(AppdynamicsDelegateService.class).to(AppdynamicsDelegateServiceImpl.class);
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(SecretManagementDelegateService.class).to(SecretManagementDelegateServiceImpl.class);
    bind(ElkDelegateService.class).to(ElkDelegateServiceImpl.class);
    bind(SplunkDelegateService.class).to(SplunkDelegateServiceImpl.class);
    bind(SumoDelegateService.class).to(SumoDelegateServiceImpl.class);
    bind(ArtifactoryBuildService.class).to(ArtifactoryBuildServiceImpl.class);
    bind(ArtifactoryService.class).to(ArtifactoryServiceImpl.class);
    bind(EcrBuildService.class).to(EcrBuildServiceImpl.class);
    bind(ContainerService.class).to(ContainerServiceImpl.class);
    bind(PcfDeploymentManager.class).to(PcfDeploymentManagerImpl.class);
    bind(PcfClient.class).to(PcfClientImpl.class);
    DelegateLogService mockDelegateLogService = mock(DelegateLogService.class);
    bind(DelegateLogService.class).toInstance(mockDelegateLogService);

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollector"))
        .toInstance(Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("systemExecutor"))
        .toInstance(ThreadPool.create(2, 5, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("system-%d").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("asyncExecutor"))
        .toInstance(ThreadPool.create(2, 20, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("async-task-%d").setPriority(Thread.MIN_PRIORITY).build()));
  }
}
