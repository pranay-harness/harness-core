package software.wings.service.impl;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import software.wings.beans.Deployment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.DeploymentService;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * The Class DeploymentServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class DeploymentServiceImpl implements DeploymentService {
  @Inject private ExecutorService executorService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private DeploymentExecutor.Factory deploymentExecutorFactory;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.DeploymentService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Deployment> list(PageRequest<Deployment> req) {
    return wingsPersistence.query(Deployment.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.DeploymentService#create(software.wings.beans.Deployment)
   */
  @Override
  public Deployment create(Deployment deployment) {
    deployment = wingsPersistence.saveAndGet(Deployment.class, deployment);
    executorService.submit(deploymentExecutorFactory.create(deployment));
    return deployment;
  }

  /**
   * The Class DeploymentExecutor.
   */
  public static class DeploymentExecutor implements Runnable {
    private Deployment deployment;

    @Inject private CommandUnitExecutorService commandUnitExecutorService;

    /**
     * Instantiates a new deployment executor.
     *
     * @param deployment the deployment
     */
    @AssistedInject
    public DeploymentExecutor(@Assisted Deployment deployment) {
      this.deployment = deployment;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      //      commandUnitExecutorService.execute(deployment);
    }

    /**
     * The Interface Factory.
     */
    public interface Factory {
      /**
       * Creates the.
       *
       * @param deployment the deployment
       * @return the deployment executor
       */
      DeploymentExecutor create(@Assisted Deployment deployment);
    }
  }
}
