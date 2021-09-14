/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
}
