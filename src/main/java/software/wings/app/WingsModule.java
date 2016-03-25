/**
 *
 */
package software.wings.app;

import com.google.inject.AbstractModule;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AppServiceImpl;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.DeploymentServiceImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.InfraServiceImpl;
import software.wings.service.impl.NodeSetExecutorServiceImpl;
import software.wings.service.impl.PlatformServiceImpl;
import software.wings.service.impl.ReleaseServiceImpl;
import software.wings.service.impl.SSHNodeSetExecutorServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.NodeSetExecutorService;
import software.wings.service.intfc.PlatformService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SSHNodeSetExecutorService;

/**
 * @author Rishi
 *
 */
public class WingsModule extends AbstractModule {
  private MainConfiguration configuration;

  /**
   * @param configuration
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
    bind(InfraService.class).to(InfraServiceImpl.class);
    bind(NodeSetExecutorService.class).to(NodeSetExecutorServiceImpl.class);
    bind(SSHNodeSetExecutorService.class).to(SSHNodeSetExecutorServiceImpl.class);
    bind(PlatformService.class).to(PlatformServiceImpl.class);
    bind(ReleaseService.class).to(ReleaseServiceImpl.class);
  }
}
