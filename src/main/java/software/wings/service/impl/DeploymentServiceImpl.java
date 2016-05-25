package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Deployment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.SshCommandUnitExecutorService;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

@Singleton
public class DeploymentServiceImpl implements DeploymentService {
  @Inject private ExecutorService executorService;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Deployment> list(PageRequest<Deployment> req) {
    return wingsPersistence.query(Deployment.class, req);
  }

  @Override
  public Deployment create(Deployment deployment) {
    deployment = wingsPersistence.saveAndGet(Deployment.class, deployment);
    executorService.submit(new DeploymentExecutor(deployment));
    return deployment;
  }

  static class DeploymentExecutor implements Runnable {
    private Deployment deployment;

    public DeploymentExecutor(Deployment deployment) {
      this.deployment = deployment;
    }

    @Override
    public void run() {
      SshCommandUnitExecutorService commandUnitExecutorService =
          WingsBootstrap.lookup(SshCommandUnitExecutorService.class);
      commandUnitExecutorService.execute(deployment);
    }
  }
}
