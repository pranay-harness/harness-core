package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.Deployment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.common.thread.ThreadPool;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.NodeSetExecutorService;

public class DeploymentServiceImpl implements DeploymentService {
  private Datastore datastore;

  public DeploymentServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public PageResponse<Deployment> list(PageRequest<Deployment> req) {
    return MongoHelper.queryPageRequest(datastore, Deployment.class, req);
  }

  @Override
  public Deployment create(Deployment deployment) {
    Key<Deployment> key = datastore.save(deployment);
    deployment = datastore.get(Deployment.class, key.getId());
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
