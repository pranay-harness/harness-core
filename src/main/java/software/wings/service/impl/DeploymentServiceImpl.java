package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Deployment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.common.thread.ThreadPool;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.NodeSetExecutorService;

@Singleton
public class DeploymentServiceImpl implements DeploymentService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Deployment> list(PageRequest<Deployment> req) {
    return wingsPersistence.query(Deployment.class, req);
  }

  @Override
  public Deployment create(Deployment deployment) {
    deployment = wingsPersistence.saveAndGet(Deployment.class, deployment);
    ThreadPool.execute(new DeploymentExecutor(deployment));
    return deployment;
  }
}

class DeploymentExecutor implements Runnable {
  private Deployment deployment;

  public DeploymentExecutor(Deployment deployment) {
    this.deployment = deployment;
  }

  @Override
  public void run() {
    NodeSetExecutorService nodeSetExecutorService = WingsBootstrap.lookup(NodeSetExecutorService.class);
    nodeSetExecutorService.execute(deployment);
  }
}
