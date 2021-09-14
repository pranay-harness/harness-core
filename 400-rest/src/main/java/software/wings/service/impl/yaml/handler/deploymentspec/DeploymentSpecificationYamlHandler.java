/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
