package software.wings.service.intfc;

import software.wings.beans.Deployment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * The Interface DeploymentService.
 */
public interface DeploymentService {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  public PageResponse<Deployment> list(PageRequest<Deployment> req);

  /**
   * Creates the.
   *
   * @param deployment the deployment
   * @return the deployment
   */
  public Deployment create(Deployment deployment);
}
