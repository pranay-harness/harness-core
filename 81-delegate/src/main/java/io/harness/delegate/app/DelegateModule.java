package io.harness.delegate.app;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import io.harness.delegate.message.MessageService;
import io.harness.delegate.message.MessageServiceImpl;
import io.harness.delegate.message.MessengerType;
import io.harness.delegate.service.DelegateCVActivityLogServiceImpl;
import io.harness.delegate.service.DelegateCVTaskServiceImpl;
import io.harness.delegate.service.DelegateConfigServiceImpl;
import io.harness.delegate.service.DelegateFileManagerImpl;
import io.harness.delegate.service.DelegateLogServiceImpl;
import io.harness.delegate.service.DelegateService;
import io.harness.delegate.service.DelegateServiceImpl;
import io.harness.delegate.service.K8sGlobalConfigServiceImpl;
import io.harness.delegate.service.LogAnalysisStoreServiceImpl;
import io.harness.delegate.service.MetricDataStoreServiceImpl;
import io.harness.govern.DependencyModule;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.shell.ShellExecutionService;
import io.harness.shell.ShellExecutionServiceImpl;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.SpotInstHelperServiceDelegateImpl;
import io.harness.threading.ThreadPool;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
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
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateCVTaskService;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenRoute53DNSWeightHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenRoute53SetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenSetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsListenerUpdateBGTaskHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsSetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.deploy.EcsDeployCommandHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sApplyTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sBlueGreenDeployTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sCanaryDeployTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sDeleteTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sInstanceSyncTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sRollingDeployRollbackTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sRollingDeployTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sScaleTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sTaskHandler;
import software.wings.delegatetasks.k8s.taskhandler.K8sTrafficSplitTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfApplicationDetailsCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCreatePcfResourceCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDataFetchCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDeployCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRollbackCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRouteUpdateCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRunPluginCommandTaskHandler;
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
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsServiceImpl;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClientImpl;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.customrepository.CustomRepositoryService;
import software.wings.helpers.ext.customrepository.CustomRepositoryServiceImpl;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl;
import software.wings.helpers.ext.docker.client.DockerRestClientFactory;
import software.wings.helpers.ext.docker.client.DockerRestClientFactoryImpl;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrClassicServiceImpl;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.ecr.EcrServiceImpl;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType;
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
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.kustomize.KustomizeClient;
import software.wings.helpers.ext.kustomize.KustomizeClientImpl;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.helpers.ext.nexus.NexusServiceImpl;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManagerImpl;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.sftp.SftpService;
import software.wings.helpers.ext.sftp.SftpServiceImpl;
import software.wings.helpers.ext.smb.SmbService;
import software.wings.helpers.ext.smb.SmbServiceImpl;
import software.wings.helpers.ext.terraform.TerraformConfigInspectClient;
import software.wings.helpers.ext.terraform.TerraformConfigInspectClientImpl;
import software.wings.service.EcrClassicBuildServiceImpl;
import software.wings.service.impl.AcrBuildServiceImpl;
import software.wings.service.impl.AmazonS3BuildServiceImpl;
import software.wings.service.impl.AmiBuildServiceImpl;
import software.wings.service.impl.ArtifactoryBuildServiceImpl;
import software.wings.service.impl.AzureArtifactsBuildServiceImpl;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.CodeDeployCommandUnitExecutorServiceImpl;
import software.wings.service.impl.ContainerCommandUnitExecutorServiceImpl;
import software.wings.service.impl.ContainerServiceImpl;
import software.wings.service.impl.CustomBuildServiceImpl;
import software.wings.service.impl.DockerBuildServiceImpl;
import software.wings.service.impl.EcrBuildServiceImpl;
import software.wings.service.impl.GcrBuildServiceImpl;
import software.wings.service.impl.GcsBuildServiceImpl;
import software.wings.service.impl.GitServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.NexusBuildServiceImpl;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.impl.SftpBuildServiceImpl;
import software.wings.service.impl.SlackMessageSenderImpl;
import software.wings.service.impl.SmbBuildServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.impl.TerraformConfigInspectServiceImpl;
import software.wings.service.impl.WinRMCommandUnitExecutorServiceImpl;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.APMDelegateServiceImpl;
import software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl;
import software.wings.service.impl.aws.delegate.AwsAmiHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsAppAutoScalingHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsAsgHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsCFHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsCloudWatchHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsCodeDeployHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEc2HelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEcrHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsEcsHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsElbHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsIamHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsLambdaHelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsRoute53HelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsS3HelperServiceDelegateImpl;
import software.wings.service.impl.aws.delegate.AwsServiceDiscoveryHelperServiceDelegateImpl;
import software.wings.service.impl.bugsnag.BugsnagDelegateService;
import software.wings.service.impl.bugsnag.BugsnagDelegateServiceImpl;
import software.wings.service.impl.cloudwatch.CloudWatchDelegateServiceImpl;
import software.wings.service.impl.dynatrace.DynaTraceDelegateServiceImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.instana.InstanaDelegateServiceImpl;
import software.wings.service.impl.ldap.LdapDelegateServiceImpl;
import software.wings.service.impl.logz.LogzDelegateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.prometheus.PrometheusDelegateServiceImpl;
import software.wings.service.impl.security.DelegateDecryptionServiceImpl;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkDelegateServiceImpl;
import software.wings.service.impl.stackdriver.StackDriverDelegateServiceImpl;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.AcrBuildService;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.AmiBuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.AzureArtifactsBuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.CustomBuildService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.EcrClassicBuildService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.GcsBuildService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.NexusBuildService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.SftpBuildService;
import software.wings.service.intfc.SlackMessageSender;
import software.wings.service.intfc.SmbBuildService;
import software.wings.service.intfc.TerraformConfigInspectService;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsCodeDeployHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsRoute53HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsS3HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsServiceDiscoveryHelperServiceDelegate;
import software.wings.service.intfc.cloudwatch.CloudWatchDelegateService;
import software.wings.service.intfc.dynatrace.DynaTraceDelegateService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.settings.SettingValue;
import software.wings.utils.HostValidationService;
import software.wings.utils.HostValidationServiceImpl;

import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateModule extends DependencyModule {
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
        .toInstance(new ScheduledThreadPoolExecutor(4,
            new ThreadFactoryBuilder().setNameFormat("TaskPoll-Thread-%d").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("inputExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("InputCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("installCheckExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("InstallCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("verificationExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(2,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollector"))
        .toInstance(ThreadPool.create(4, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("systemExecutor"))
        .toInstance(ThreadPool.create(4, 9, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("system-%d").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("asyncExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("async-task-%d").setPriority(Thread.MIN_PRIORITY).build()));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("alternativeExecutor"))
        .toInstance(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("alternative-validation-task-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

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
    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    bind(DelegateFileManager.class).to(DelegateFileManagerImpl.class).asEagerSingleton();
    bind(ServiceCommandExecutorService.class).to(ServiceCommandExecutorServiceImpl.class);
    bind(SshExecutorFactory.class);
    bind(DelegateLogService.class).to(DelegateLogServiceImpl.class);
    bind(MetricDataStoreService.class).to(MetricDataStoreServiceImpl.class);
    bind(LogAnalysisStoreService.class).to(LogAnalysisStoreServiceImpl.class);
    bind(DelegateCVTaskService.class).to(DelegateCVTaskServiceImpl.class);
    bind(DelegateCVActivityLogService.class).to(DelegateCVActivityLogServiceImpl.class);
    bind(DelegateConfigService.class).to(DelegateConfigServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
    bind(SmbBuildService.class).to(SmbBuildServiceImpl.class);
    bind(SmbService.class).to(SmbServiceImpl.class);
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
    bind(AwsCodeDeployService.class).to(AwsCodeDeployServiceImpl.class);
    bind(NexusBuildService.class).to(NexusBuildServiceImpl.class);
    bind(NexusService.class).to(NexusServiceImpl.class);
    bind(AppdynamicsDelegateService.class).to(AppdynamicsDelegateServiceImpl.class);
    bind(InstanaDelegateService.class).to(InstanaDelegateServiceImpl.class);
    bind(StackDriverDelegateService.class).to(StackDriverDelegateServiceImpl.class);
    bind(APMDelegateService.class).to(APMDelegateServiceImpl.class);
    bind(NewRelicDelegateService.class).to(NewRelicDelgateServiceImpl.class);
    bind(BugsnagDelegateService.class).to(BugsnagDelegateServiceImpl.class);
    bind(DynaTraceDelegateService.class).to(DynaTraceDelegateServiceImpl.class);
    bind(PrometheusDelegateService.class).to(PrometheusDelegateServiceImpl.class);
    bind(SplunkDelegateService.class).to(SplunkDelegateServiceImpl.class);
    bind(ElkDelegateService.class).to(ElkDelegateServiceImpl.class);
    bind(LogzDelegateService.class).to(LogzDelegateServiceImpl.class);
    bind(SumoDelegateService.class).to(SumoDelegateServiceImpl.class);
    bind(SplunkDelegateService.class).to(SplunkDelegateServiceImpl.class);
    bind(CloudWatchDelegateService.class).to(CloudWatchDelegateServiceImpl.class);
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
    bind(CustomBuildService.class).to(CustomBuildServiceImpl.class);
    bind(CustomRepositoryService.class).to(CustomRepositoryServiceImpl.class);
    bind(AmiService.class).to(AmiServiceImpl.class);
    bind(AzureArtifactsBuildService.class).to(AzureArtifactsBuildServiceImpl.class);
    bind(HostValidationService.class).to(HostValidationServiceImpl.class);
    bind(ContainerService.class).to(ContainerServiceImpl.class);
    bind(GitClient.class).to(GitClientImpl.class).asEagerSingleton();
    bind(SecretManagementDelegateService.class).to(SecretManagementDelegateServiceImpl.class);
    bind(KmsEncryptDecryptClient.class);
    bind(EncryptionService.class).to(EncryptionServiceImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(HelmClient.class).to(HelmClientImpl.class);
    bind(KustomizeClient.class).to(KustomizeClientImpl.class);
    bind(HelmDeployService.class).to(HelmDeployServiceImpl.class);
    bind(ContainerDeploymentDelegateHelper.class);
    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl(Clock.systemUTC(), MessengerType.DELEGATE, DelegateApplication.getProcessId()));
    bind(PcfClient.class).to(PcfClientImpl.class);
    bind(PcfDeploymentManager.class).to(PcfDeploymentManagerImpl.class);
    bind(AwsEcrHelperServiceDelegate.class).to(AwsEcrHelperServiceDelegateImpl.class);
    bind(AwsElbHelperServiceDelegate.class).to(AwsElbHelperServiceDelegateImpl.class);
    bind(AwsEcsHelperServiceDelegate.class).to(AwsEcsHelperServiceDelegateImpl.class);
    bind(AwsAppAutoScalingHelperServiceDelegate.class).to(AwsAppAutoScalingHelperServiceDelegateImpl.class);
    bind(AwsIamHelperServiceDelegate.class).to(AwsIamHelperServiceDelegateImpl.class);
    bind(AwsEc2HelperServiceDelegate.class).to(AwsEc2HelperServiceDelegateImpl.class);
    bind(AwsAsgHelperServiceDelegate.class).to(AwsAsgHelperServiceDelegateImpl.class);
    bind(AwsCodeDeployHelperServiceDelegate.class).to(AwsCodeDeployHelperServiceDelegateImpl.class);
    bind(AwsLambdaHelperServiceDelegate.class).to(AwsLambdaHelperServiceDelegateImpl.class);
    bind(AwsAmiHelperServiceDelegate.class).to(AwsAmiHelperServiceDelegateImpl.class);
    bind(GitService.class).to(GitServiceImpl.class);
    bind(LdapDelegateService.class).to(LdapDelegateServiceImpl.class);
    bind(AwsCFHelperServiceDelegate.class).to(AwsCFHelperServiceDelegateImpl.class);
    bind(SftpBuildService.class).to(SftpBuildServiceImpl.class);
    bind(SftpService.class).to(SftpServiceImpl.class);
    bind(K8sGlobalConfigService.class).to(K8sGlobalConfigServiceImpl.class);
    bind(DockerRestClientFactory.class).to(DockerRestClientFactoryImpl.class);
    bind(ShellExecutionService.class).to(ShellExecutionServiceImpl.class);
    bind(CustomRepositoryService.class).to(CustomRepositoryServiceImpl.class);
    bind(AwsRoute53HelperServiceDelegate.class).to(AwsRoute53HelperServiceDelegateImpl.class);
    bind(AwsServiceDiscoveryHelperServiceDelegate.class).to(AwsServiceDiscoveryHelperServiceDelegateImpl.class);
    bind(DelegateDecryptionService.class).to(DelegateDecryptionServiceImpl.class);
    bind(ServiceNowDelegateService.class).to(ServiceNowDelegateServiceImpl.class);
    bind(ChartMuseumClient.class).to(ChartMuseumClientImpl.class);
    bind(SpotInstHelperServiceDelegate.class).to(SpotInstHelperServiceDelegateImpl.class);
    bind(AwsS3HelperServiceDelegate.class).to(AwsS3HelperServiceDelegateImpl.class);

    bind(SlackMessageSender.class).to(SlackMessageSenderImpl.class);

    bind(AwsCloudWatchHelperServiceDelegate.class).to(AwsCloudWatchHelperServiceDelegateImpl.class);
    bind(AzureArtifactsService.class).to(AzureArtifactsServiceImpl.class);

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
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.CREATE_ROUTE.name())
        .to(PcfCreatePcfResourceCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.addBinding(PcfCommandType.RUN_PLUGIN.name())
        .to(PcfRunPluginCommandTaskHandler.class);

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
    buildServiceMapBinder.addBinding(AzureArtifactsPATConfig.class).toInstance(AzureArtifactsBuildService.class);

    // ECS Command Tasks
    MapBinder<String, EcsCommandTaskHandler> ecsCommandTaskTypeToTaskHandlerMap =
        MapBinder.newMapBinder(binder(), String.class, EcsCommandTaskHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.LISTENER_UPDATE_BG.name())
        .to(EcsListenerUpdateBGTaskHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.BG_SERVICE_SETUP.name())
        .to(EcsBlueGreenSetupCommandHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.ROUTE53_BG_SERVICE_SETUP.name())
        .to(EcsBlueGreenRoute53SetupCommandHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.ROUTE53_DNS_WEIGHT_UPDATE.name())
        .to(EcsBlueGreenRoute53DNSWeightHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.SERVICE_SETUP.name()).to(EcsSetupCommandHandler.class);
    ecsCommandTaskTypeToTaskHandlerMap.addBinding(EcsCommandType.SERVICE_DEPLOY.name())
        .to(EcsDeployCommandHandler.class);

    MapBinder<String, K8sTaskHandler> k8sCommandTaskTypeToTaskHandlerMap =
        MapBinder.newMapBinder(binder(), String.class, K8sTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.DEPLOYMENT_ROLLING.name())
        .to(K8sRollingDeployTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK.name())
        .to(K8sRollingDeployRollbackTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.CANARY_DEPLOY.name())
        .to(K8sCanaryDeployTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.SCALE.name()).to(K8sScaleTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.BLUE_GREEN_DEPLOY.name())
        .to(K8sBlueGreenDeployTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.INSTANCE_SYNC.name())
        .to(K8sInstanceSyncTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.DELETE.name()).to(K8sDeleteTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.TRAFFIC_SPLIT.name())
        .to(K8sTrafficSplitTaskHandler.class);
    k8sCommandTaskTypeToTaskHandlerMap.addBinding(K8sTaskType.APPLY.name()).to(K8sApplyTaskHandler.class);
    bind(TerraformConfigInspectClient.class).toInstance(new TerraformConfigInspectClientImpl());
    bind(TerraformConfigInspectService.class).toInstance(new TerraformConfigInspectServiceImpl());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(TimeModule.getInstance(), VersionModule.getInstance());
  }
}
