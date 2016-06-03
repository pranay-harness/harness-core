package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;

import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.impl.ActivityServiceImpl;
import software.wings.service.impl.AppContainerServiceImpl;
import software.wings.service.impl.AppServiceImpl;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.CatalogServiceImpl;
import software.wings.service.impl.ConfigServiceImpl;
import software.wings.service.impl.DeploymentServiceImpl;
import software.wings.service.impl.EmailNotificationServiceImpl;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.ExecutionLogsImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.HostServiceImpl;
import software.wings.service.impl.InfraServiceImpl;
import software.wings.service.impl.JenkinsArtifactCollectorServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.impl.PlatformServiceImpl;
import software.wings.service.impl.ReleaseServiceImpl;
import software.wings.service.impl.RoleServiceImpl;
import software.wings.service.impl.ServiceCommandExecutorService;
import software.wings.service.impl.ServiceInstanceServiceImpl;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.impl.TagServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PlatformService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceCommandExecutorServiceImpl;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TagService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExpressionProcessorFactory;

// TODO: Auto-generated Javadoc

/**
 * Guice Module for initializing all beans.
 *
 * @author Rishi
 */
public class WingsModule extends AbstractModule {
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
    bind(MainConfiguration.class).toInstance(configuration);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(AppService.class).to(AppServiceImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(AuditService.class).to(AuditServiceImpl.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(FileService.class).to(FileServiceImpl.class);
    bind(CommandUnitExecutorService.class).to(SshCommandUnitExecutorServiceImpl.class);
    bind(PlatformService.class).to(PlatformServiceImpl.class);
    bind(ReleaseService.class).to(ReleaseServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(RoleService.class).to(RoleServiceImpl.class);
    bind(ServiceResourceService.class).to(ServiceResourceServiceImpl.class);
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
    bind(ServiceTemplateService.class).to(ServiceTemplateServiceImpl.class);
    bind(InfraService.class).to(InfraServiceImpl.class);
    bind(WorkflowService.class).to(WorkflowServiceImpl.class);
    bind(PluginManager.class).to(DefaultPluginManager.class).asEagerSingleton();
    bind(TagService.class).to(TagServiceImpl.class);
    bind(ConfigService.class).to(ConfigServiceImpl.class);
    bind(AppContainerService.class).to(AppContainerServiceImpl.class);
    bind(CatalogService.class).to(CatalogServiceImpl.class);
    bind(HostService.class).to(HostServiceImpl.class);
    bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
    bind(ExecutionLogs.class).to(ExecutionLogsImpl.class);
    bind(SettingsService.class).to(SettingsServiceImpl.class);
    bind(ExpressionProcessorFactory.class).to(WingsExpressionProcessorFactory.class);
    bind(CommandUnitExecutorService.class).to(SshCommandUnitExecutorServiceImpl.class);
    bind(SshExecutorFactory.class);
    bind(new TypeLiteral<NotificationService<EmailData>>() {}).to(EmailNotificationServiceImpl.class);
    bind(ServiceInstanceService.class).to(ServiceInstanceServiceImpl.class);
    bind(new TypeLiteral<NotificationService<EmailData>>() {}).to(EmailNotificationServiceImpl.class);
    bind(ActivityService.class).to(ActivityServiceImpl.class);
    bind(LogService.class).to(LogServiceImpl.class);
    bind(ServiceCommandExecutorService.class).to(ServiceCommandExecutorServiceImpl.class);

    MapBinder<String, ArtifactCollectorService> artifactCollectorServiceMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ArtifactCollectorService.class);
    artifactCollectorServiceMapBinder.addBinding(SourceType.JENKINS.name())
        .to(JenkinsArtifactCollectorServiceImpl.class);

    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    install(new FactoryModuleBuilder().build(DeploymentServiceImpl.DeploymentExecutor.Factory.class));
  }
}
