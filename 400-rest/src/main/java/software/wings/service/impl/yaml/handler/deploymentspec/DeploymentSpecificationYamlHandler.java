/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.deploymentspec;

import software.wings.beans.DeploymentSpecification;
import software.wings.beans.yaml.ChangeContext;
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
  public void delete(ChangeContext<Y> changeContext) {
    // do nothing
  }
}
