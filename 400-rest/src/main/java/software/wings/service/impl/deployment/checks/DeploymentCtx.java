/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.deployment.checks;

import java.util.List;
import lombok.Value;

/**
 * Deployment Context POJO.
 */
@Value
public class DeploymentCtx {
  // a deployment can be associated with only one appIds
  private String appId;

  // a pipeline deployment can be associated with multiple envIds
  private List<String> envIds;

  // a deployment can be associated with multiple serviceIds
  private List<String> serviceIds;
}
