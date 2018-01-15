package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.beans.DeploymentSpecification;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;

/**
 * Base yaml handler for all deployment specifications
 * @author rktummala on 11/16/17
 */
public abstract class DeploymentSpecificationYamlHandler<Y extends DeploymentSpecification.Yaml, B
                                                             extends DeploymentSpecification>
    extends BaseYamlHandler<Y, B> {
  // We should not allow deletion of any deployment spec from the service
  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    // do nothing
  }
}
