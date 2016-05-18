package software.wings.service.intfc;

import software.wings.beans.Deployment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

public interface DeploymentService {
  public PageResponse<Deployment> list(PageRequest<Deployment> req);

  public Deployment create(Deployment deployment);
}
