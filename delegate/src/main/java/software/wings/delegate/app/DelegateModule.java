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
import software.wings.api.DeploymentType;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.AwsClusterServiceImpl;
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
import software.wings.delegate.service.UpgradeService;
import software.wings.delegate.service.UpgradeServiceImpl;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.BambooServiceImpl;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;
import software.wings.service.impl.BambooBuildServiceImpl;
import software.wings.service.impl.DockerBuildServiceImpl;
import software.wings.service.impl.EcsCommandUnitExecutorServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.util.concurrent.ExecutorService;
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
        .annotatedWith(Names.named("upgradeExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("UpgradeCheck-Thread").setPriority(Thread.MAX_PRIORITY).build()));

    int cores = Runtime.getRuntime().availableProcessors();
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(2 * cores, 2 * cores, 0, TimeUnit.MILLISECONDS,
            new ThreadFactoryBuilder().setNameFormat("delegate-task-%d").build()));
    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    bind(DelegateFileManager.class).to(DelegateFileManagerImpl.class);
    bind(UpgradeService.class).to(UpgradeServiceImpl.class);
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    bind(ServiceCommandExecutorService.class).to(ServiceCommandExecutorServiceImpl.class);
    bind(SshExecutorFactory.class);
    bind(DelegateLogService.class).to(DelegateLogServiceImpl.class);
    bind(DelegateConfigService.class).to(DelegateConfigServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
    bind(BambooBuildService.class).to(BambooBuildServiceImpl.class);
    bind(DockerBuildService.class).to(DockerBuildServiceImpl.class);
    bind(BambooService.class).to(BambooServiceImpl.class);
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(AsyncHttpClient.class)
        .toInstance(new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build()));
    bind(AwsClusterService.class).to(AwsClusterServiceImpl.class);
    bind(EcsContainerService.class).to(EcsContainerServiceImpl.class);
    bind(GkeClusterService.class).to(GkeClusterServiceImpl.class);
    bind(KubernetesContainerService.class).to(KubernetesContainerServiceImpl.class);

    MapBinder<String, CommandUnitExecutorService> serviceCommandExecutorServiceMapBinder =
        MapBinder.newMapBinder(binder(), String.class, CommandUnitExecutorService.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.ECS.name())
        .to(EcsCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.KUBERNETES.name())
        .to(EcsCommandUnitExecutorServiceImpl.class);
    serviceCommandExecutorServiceMapBinder.addBinding(DeploymentType.SSH.name())
        .to(SshCommandUnitExecutorServiceImpl.class);
  }
}
