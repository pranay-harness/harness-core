package software.wings;

import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import io.harness.manage.ManagedExecutorService;
import io.harness.shell.ShellExecutionService;
import io.harness.shell.ShellExecutionServiceImpl;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.SpotInstHelperServiceDelegateImpl;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import software.wings.app.MainConfiguration;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.artifactory.ArtifactoryServiceImpl;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsServiceImpl;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClientImpl;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl;
import software.wings.helpers.ext.docker.client.DockerRestClientFactory;
import software.wings.helpers.ext.docker.client.DockerRestClientFactoryImpl;
import software.wings.helpers.ext.kustomize.KustomizeClient;
import software.wings.helpers.ext.kustomize.KustomizeClientImpl;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.helpers.ext.nexus.NexusServiceImpl;
import software.wings.helpers.ext.openshift.OpenShiftClient;
import software.wings.helpers.ext.openshift.OpenShiftClientImpl;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerImpl;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.ElasticsearchDao;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntity;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.ArtifactoryBuildServiceImpl;
import software.wings.service.impl.AzureArtifactsBuildServiceImpl;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.DockerBuildServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.GitServiceImpl;
import software.wings.service.impl.NexusBuildServiceImpl;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.APMDelegateServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl;
import software.wings.service.impl.aws.delegate.AwsAppAutoScalingHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsAsgHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsCFHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEc2HelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEcsHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsElbHelperServiceDelegateImpl;
import software.wings.service.impl.aws.manager.AwsS3HelperServiceManagerImpl;
import software.wings.service.impl.cloudwatch.CloudWatchDelegateServiceImpl;
import software.wings.service.impl.dynatrace.DynaTraceDelegateServiceImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.instana.InstanaDelegateServiceImpl;
import software.wings.service.impl.logz.LogzDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.stackdriver.StackDriverDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.AzureArtifactsBuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.service.intfc.aws.manager.AwsS3HelperServiceManager;
import software.wings.service.intfc.cloudwatch.CloudWatchDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WingsTestModule extends AbstractModule {
  @Override
  protected void configure() {
    DelegateFileManager mockDelegateFileManager = mock(DelegateFileManager.class);
    bind(DelegateFileManager.class).toInstance(mockDelegateFileManager);
    bind(AmazonS3BuildService.class).to(AmazonS3BuildServiceImpl.class);
    bind(AmazonS3Service.class).to(AmazonS3ServiceImpl.class);
    bind(NewRelicDelegateService.class).to(NewRelicDelgateServiceImpl.class);
    bind(AppdynamicsDelegateService.class).to(AppdynamicsDelegateServiceImpl.class);
    bind(DynaTraceDelegateService.class).to(DynaTraceDelegateServiceImpl.class);
    bind(APMDelegateService.class).to(APMDelegateServiceImpl.class);
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(SecretManagementDelegateService.class).to(SecretManagementDelegateServiceImpl.class);
    bind(ElkDelegateService.class).to(ElkDelegateServiceImpl.class);
    bind(LogzDelegateService.class).to(LogzDelegateServiceImpl.class);
    bind(SplunkDelegateService.class).to(SplunkDelegateServiceImpl.class);
    bind(SumoDelegateService.class).to(SumoDelegateServiceImpl.class);
    bind(InstanaDelegateService.class).to(InstanaDelegateServiceImpl.class);
    bind(StackDriverDelegateService.class).to(StackDriverDelegateServiceImpl.class);
    bind(ArtifactoryBuildService.class).to(ArtifactoryBuildServiceImpl.class);
    bind(ArtifactoryService.class).to(ArtifactoryServiceImpl.class);
    bind(NexusService.class).to(NexusServiceImpl.class);
    bind(NexusBuildService.class).to(NexusBuildServiceImpl.class);
    bind(BambooBuildService.class).to(BambooBuildServiceImpl.class);
    bind(BambooService.class).to(BambooServiceImpl.class);
    bind(DockerBuildService.class).to(DockerBuildServiceImpl.class);
    bind(DockerRestClientFactory.class).to(DockerRestClientFactoryImpl.class);
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(EcrBuildService.class).to(EcrBuildServiceImpl.class);
    bind(ContainerService.class).to(ContainerServiceImpl.class);
    bind(AwsAppAutoScalingHelperServiceDelegate.class).to(AwsAppAutoScalingHelperServiceDelegateImpl.class);
    bind(AwsElbHelperServiceDelegate.class).to(AwsElbHelperServiceDelegateImpl.class);
    bind(SpotInstHelperServiceDelegate.class).to(SpotInstHelperServiceDelegateImpl.class);
    bind(AwsAsgHelperServiceDelegate.class).to(AwsAsgHelperServiceDelegateImpl.class);
    bind(AwsEc2HelperServiceDelegate.class).to(AwsEc2HelperServiceDelegateImpl.class);
    bind(AwsEcsHelperServiceDelegate.class).to(AwsEcsHelperServiceDelegateImpl.class);
    bind(GitService.class).to(GitServiceImpl.class);
    bind(KustomizeClient.class).to(KustomizeClientImpl.class);
    bind(OpenShiftClient.class).to(OpenShiftClientImpl.class);
    bind(ChartMuseumClient.class).to(ChartMuseumClientImpl.class);
    bind(SearchDao.class).to(ElasticsearchDao.class);
    bind(PcfClient.class).to(PcfClientImpl.class);
    DelegateLogService mockDelegateLogService = mock(DelegateLogService.class);
    bind(DelegateLogService.class).toInstance(mockDelegateLogService);
    DelegateCVActivityLogService mockDelegateCVActivityLogService = mock(DelegateCVActivityLogService.class);
    bind(DelegateCVActivityLogService.class).toInstance(mockDelegateCVActivityLogService);
    GitClientHelper gitClientHelper = mock(GitClientHelper.class);
    bind(GitClientImpl.class);
    bind(PcfDeploymentManagerImpl.class);
    bind(AwsCFHelperServiceDelegate.class).to(AwsCFHelperServiceDelegateImpl.class);
    bind(AwsS3HelperServiceManager.class).to(AwsS3HelperServiceManagerImpl.class);
    bind(AzureArtifactsService.class).to(AzureArtifactsServiceImpl.class);
    bind(AzureArtifactsBuildService.class).to(AzureArtifactsBuildServiceImpl.class);
    bind(CloudWatchDelegateService.class).to(CloudWatchDelegateServiceImpl.class);

    bind(ExecutorService.class)
        .annotatedWith(Names.named("systemExecutor"))
        .toInstance(ThreadPool.create(4, 8, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("system-%d").setPriority(Thread.MAX_PRIORITY).build()));
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
    bind(ManagedExecutorService.class)
        .toInstance(new ManagedExecutorService(ThreadPool.create(1, 1, 0, TimeUnit.SECONDS)));
    bind(ShellExecutionService.class).to(ShellExecutionServiceImpl.class);

    Multibinder<SearchEntity<?>> searchEntityMultibinder =
        Multibinder.newSetBinder(binder(), new TypeLiteral<SearchEntity<?>>() {});
    searchEntityMultibinder.addBinding().to(ApplicationSearchEntity.class);
    searchEntityMultibinder.addBinding().to(DeploymentSearchEntity.class);
    searchEntityMultibinder.addBinding().to(ServiceSearchEntity.class);
    searchEntityMultibinder.addBinding().to(EnvironmentSearchEntity.class);
    searchEntityMultibinder.addBinding().to(WorkflowSearchEntity.class);
    searchEntityMultibinder.addBinding().to(PipelineSearchEntity.class);
  }

  @Provides
  @Singleton
  public RestHighLevelClient getElasticsearchClient(MainConfiguration mainConfiguration) {
    try {
      URI uri = new URIBuilder(mainConfiguration.getElasticsearchConfig().getUri()).build();
      return new RestHighLevelClient(RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())));
    } catch (URISyntaxException e) {
      logger.error(
          String.format("Elasticsearch URI %s is invalid", mainConfiguration.getElasticsearchConfig().getUri()), e);
    }
    return null;
  }
}
