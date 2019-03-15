package software.wings;

import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.shell.ShellExecutionService;
import io.harness.shell.ShellExecutionServiceImpl;
import io.harness.threading.ThreadPool;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.artifactory.ArtifactoryServiceImpl;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl;
import software.wings.helpers.ext.docker.client.DockerRestClientFactory;
import software.wings.helpers.ext.docker.client.DockerRestClientFactoryImpl;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.helpers.ext.nexus.NexusServiceImpl;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.ArtifactoryBuildServiceImpl;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.DockerBuildServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.NexusBuildServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl;
import software.wings.service.impl.aws.delegate.AwsAppAutoScalingHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsAsgHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsCFHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEc2HelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEcsHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsElbHelperServiceDelegateImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.util.concurrent.ExecutorService;
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
    bind(NexusService.class).to(NexusServiceImpl.class);
    bind(NexusBuildService.class).to(NexusBuildServiceImpl.class);
    bind(DockerBuildService.class).to(DockerBuildServiceImpl.class);
    bind(DockerRestClientFactory.class).to(DockerRestClientFactoryImpl.class);
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(EcrBuildService.class).to(EcrBuildServiceImpl.class);
    bind(ContainerService.class).to(ContainerServiceImpl.class);
    bind(AwsAppAutoScalingHelperServiceDelegate.class).to(AwsAppAutoScalingHelperServiceDelegateImpl.class);
    bind(AwsElbHelperServiceDelegate.class).to(AwsElbHelperServiceDelegateImpl.class);
    bind(AwsAsgHelperServiceDelegate.class).to(AwsAsgHelperServiceDelegateImpl.class);
    bind(AwsEc2HelperServiceDelegate.class).to(AwsEc2HelperServiceDelegateImpl.class);
    bind(AwsEcsHelperServiceDelegate.class).to(AwsEcsHelperServiceDelegateImpl.class);

    bind(PcfClient.class).to(PcfClientImpl.class);
    DelegateLogService mockDelegateLogService = mock(DelegateLogService.class);
    bind(DelegateLogService.class).toInstance(mockDelegateLogService);
    GitClientHelper gitClientHelper = mock(GitClientHelper.class);
    bind(GitClientImpl.class);
    bind(PcfDeploymentManagerImpl.class);
    bind(AwsCFHelperServiceDelegate.class).to(AwsCFHelperServiceDelegateImpl.class);

    bind(ExecutorService.class)
        .annotatedWith(Names.named("systemExecutor"))
        .toInstance(ThreadPool.create(4, 8, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("system-%d").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("asyncExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("async-task-%d").setPriority(Thread.MIN_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("artifactExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("artifact-collection-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("timeoutExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("timeout-enforcer-%d").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ShellExecutionService.class).to(ShellExecutionServiceImpl.class);
  }
}
