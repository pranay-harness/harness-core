package software.wings.service.impl;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

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

  @Inject private DeploymentExecutor.Factory deploymentExecutorFactory;

  @Override
  public PageResponse<Deployment> list(PageRequest<Deployment> req) {
    return wingsPersistence.query(Deployment.class, req);
  }

  @Override
  public Deployment create(Deployment deployment) {
    deployment = wingsPersistence.saveAndGet(Deployment.class, deployment);
    executorService.submit(deploymentExecutorFactory.create(deployment));
    return deployment;
  }

  public static class DeploymentExecutor implements Runnable {
    private Deployment deployment;

    @Inject private SshCommandUnitExecutorService sshCommandUnitExecutorService;

    @AssistedInject
    public DeploymentExecutor(@Assisted Deployment deployment) {
      this.deployment = deployment;
    }

    @Override
    public void run() {
      sshCommandUnitExecutorService.execute(deployment);
    }

    public interface Factory { DeploymentExecutor create(@Assisted Deployment deployment); }
  }
}
