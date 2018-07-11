package software.wings.delegate.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;
import software.wings.api.DeploymentType;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.AwsClusterServiceImpl;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.cloudprovider.aws.AwsCodeDeployServiceImpl;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.cloudprovider.aws.EcsContainerServiceImpl;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;
import software.wings.common.thread.ThreadPool;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.delegate.service.DelegateConfigServiceImpl;
import software.wings.delegate.service.DelegateFileManagerImpl;
import software.wings.delegate.service.DelegateLogServiceImpl;
import software.wings.delegate.service.DelegateService;
import software.wings.delegate.service.DelegateServiceImpl;
import software.wings.delegate.service.LogAnalysisStoreServiceImpl;
import software.wings.delegate.service.MetricDataStoreServiceImpl;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfApplicationDetailsCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDataFetchCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDeployCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRollbackCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRouteUpdateCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfSetupCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfValidationCommandTaskHandler;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.amazons3.AmazonS3ServiceImpl;
import software.wings.helpers.ext.ami.AmiService;
import software.wings.helpers.ext.ami.AmiServiceImpl;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.artifactory.ArtifactoryServiceImpl;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.helpers.ext.azure.AcrServiceImpl;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrClassicServiceImpl;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.ecr.EcrServiceImpl;
import software.wings.helpers.ext.gcr.GcrService;
import software.wings.helpers.ext.gcr.GcrServiceImpl;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.gcs.GcsServiceImpl;
import software.wings.helpers.ext.helm.HelmClient;
import software.wings.helpers.ext.helm.HelmClientImpl;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.HelmDeployServiceImpl;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.helpers.ext.nexus.NexusServiceImpl;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerImpl;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.service.EcrClassicBuildServiceImpl;
import software.wings.service.impl.AcrBuildServiceImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.AmiBuildServiceImpl;
import software.wings.service.impl.ArtifactoryBuildServiceImpl;
import software.wings.service.impl.AwsEc2ServiceImpl;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.CodeDeployCommandUnitExecutorServiceImpl;
import software.wings.service.impl.ContainerCommandUnitExecutorServiceImpl;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.DockerBuildServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.GcrBuildServiceImpl;
import software.wings.service.impl.GcsBuildServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.NexusBuildServiceImpl;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.impl.WinRMCommandUnitExecutorServiceImpl;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.APMDelegateServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl;
import software.wings.service.impl.aws.delegate.AwsEcrHelperServiceDelegateImpl;
import software.wings.service.impl.dynatrace.DynaTraceDelegateServiceImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.logz.LogzDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.prometheus.PrometheusDelegateServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.AcrBuildService;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.AmiBuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.AwsEc2Service;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.EcrClassicBuildService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.GcsBuildService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.utils.HostValidationService;
import software.wings.utils.HostValidationServiceImpl;
import software.wings.utils.message.MessageService;
import software.wings.utils.message.MessageServiceImpl;
import software.wings.utils.message.MessengerType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateService.class).to(DelegateServiceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("heartbeatExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Heartbeat-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("localHeartbeatExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("LocalHeartbeat-Thread")
                .setPriority(Thread.MAX_PRIORITY)
                .build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("upgradeExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("UpgradeCheck-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("TaskPoll-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("inputExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("InputCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("verificationExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(2,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollector"))
        .toInstance(Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("systemExecutor"))
        .toInstance(ThreadPool.create(4, 15, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("system-%d").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("asyncExecutor"))
        .toInstance(ThreadPool.create(10, 50, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("async-task-%d").setPriority(Thread.MIN_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("artifactExecutor"))
        .toInstance(ThreadPool.create(10, 50, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("artifact-collection-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("timeoutExecutor"))
        .toInstance(ThreadPool.create(10, 150, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("timeout-enforcer-%d").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(10, 50, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("sync-task-%d").setPriority(Thread.NORM_PRIORITY).build()));

    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    bind(DelegateFileManager.class).to(DelegateFileManagerImpl.class).asEagerSingleton();
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    bind(ServiceCommandExecutorService.class).to(ServiceCommandExecutorServiceImpl.class);
    bind(SshExecutorFactory.class);
    bind(DelegateLogService.class).to(DelegateLogServiceImpl.class);
    bind(MetricDataStoreService.class).to(MetricDataStoreServiceImpl.class);
    bind(LogAnalysisStoreService.class).to(LogAnalysisStoreServiceImpl.class);
    bind(DelegateConfigService.class).to(DelegateConfigServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
    bind(BambooBuildService.class).to(BambooBuildServiceImpl.class);
    bind(DockerBuildService.class).to(DockerBuildServiceImpl.class);
    bind(BambooService.class).to(BambooServiceImpl.class);
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(AsyncHttpClient.class)
        .toInstance(new AsyncHttpClient(
            new AsyncHttpClientConfig.Builder().setUseProxyProperties(true).setAcceptAnyCertificate(true).build()));
    bind(AwsClusterService.class).to(AwsClusterServiceImpl.class);
    bind(EcsContainerService.class).to(EcsContainerServiceImpl.class);
    bind(GkeClusterService.class).to(GkeClusterServiceImpl.class);
    bind(KubernetesContainerService.class).to(KubernetesContainerServiceImpl.class);
    bind(AwsCodeDeployService.class).to(AwsCodeDeployServiceImpl.class);
    bind(NexusBuildService.class).to(NexusBuildServiceImpl.class);
    bind(NexusService.class).to(NexusServiceImpl.class);
    bind(AppdynamicsDelegateService.class).to(AppdynamicsDelegateServiceImpl.class);
    bind(APMDelegateService.class).to(APMDelegateServiceImpl.class);
    bind(NewRelicDelegateService.class).to(NewRelicDelgateServiceImpl.class);
    bind(DynaTraceDelegateService.class).to(DynaTraceDelegateServiceImpl.class);
    bind(PrometheusDelegateService.class).to(PrometheusDelegateServiceImpl.class);
    bind(SplunkDelegateService.class).to(SplunkDelegateServiceImpl.class);
    bind(ElkDelegateService.class).to(ElkDelegateServiceImpl.class);
    bind(LogzDelegateService.class).to(LogzDelegateServiceImpl.class);
    bind(SumoDelegateService.class).to(SumoDelegateServiceImpl.class);
    bind(ArtifactoryBuildService.class).to(ArtifactoryBuildServiceImpl.class);
    bind(ArtifactoryService.class).to(ArtifactoryServiceImpl.class);
    bind(EcrBuildService.class).to(EcrBuildServiceImpl.class);
    bind(AmazonS3BuildService.class).to(AmazonS3BuildServiceImpl.class);
    bind(AmazonS3Service.class).to(AmazonS3ServiceImpl.class);
    bind(GcsBuildService.class).to(GcsBuildServiceImpl.class);
    bind(GcsService.class).to(GcsServiceImpl.class);
    bind(EcrClassicBuildService.class).to(EcrClassicBuildServiceImpl.class);
    bind(EcrService.class).to(EcrServiceImpl.class);
    bind(EcrClassicService.class).to(EcrClassicServiceImpl.class);
    bind(GcrService.class).to(GcrServiceImpl.class);
    bind(GcrBuildService.class).to(GcrBuildServiceImpl.class);
    bind(AcrService.class).to(AcrServiceImpl.class);
    bind(AcrBuildService.class).to(AcrBuildServiceImpl.class);
    bind(AmiBuildService.class).to(AmiBuildServiceImpl.class);
    bind(AmiService.class).to(AmiServiceImpl.class);
    bind(HostValidationService.class).to(HostValidationServiceImpl.class);
    bind(ContainerService.class).to(ContainerServiceImpl.class);
    bind(GitClient.class).to(GitClientImpl.class).asEagerSingleton();
    bind(SecretManagementDelegateService.class).to(SecretManagementDelegateServiceImpl.class);
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(HelmClient.class).to(HelmClientImpl.class);
    bind(HelmDeployService.class).to(HelmDeployServiceImpl.class);
    bind(ContainerDeploymentDelegateHelper.class);
    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl(Clock.systemUTC(), MessengerType.DELEGATE, DelegateApplication.getProcessId()));
    bind(PcfClient.class).to(PcfClientImpl.class);
    bind(PcfDeploymentManager.class).to(PcfDeploymentManagerImpl.class);
    bind(AwsEc2Service.class).to(AwsEc2ServiceImpl.class);
    bind(AwsEcrHelperServiceDelegate.class).to(AwsEcrHelperServiceDelegateImpl.class);

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new RuntimeException("Could not load versionInfo.yaml", e);
    }

    MapBinder<String, CommandUnitExecutorService> serviceCommandExecutorServiceMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CommandUnitExecutorService.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.ECS.name())
        .to(ContainerCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.KUBERNETES.name())
        .to(ContainerCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.SSH.name())
        .to(SshCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.WINRM.name())
        .to(WinRMCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.AWS_CODEDEPLOY.name())
        .to(CodeDeployCommandUnitExecutorServiceImpl.class);

    MapBinder<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap =
        MapBinder.newMapBinder(binder(), String.class, PcfCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.SETUP.name()).to(PcfSetupCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.RESIZE.name()).to(PcfDeployCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.ROLLBACK.name()).to(PcfRollbackCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.UPDATE_ROUTE.name())
        .to(PcfRouteUpdateCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.VALIDATE.name())
        .to(PcfValidationCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.APP_DETAILS.name())
        .to(PcfApplicationDetailsCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.DATAFETCH.name())
        .to(PcfDataFetchCommandTaskHandler.class);
  }
}
