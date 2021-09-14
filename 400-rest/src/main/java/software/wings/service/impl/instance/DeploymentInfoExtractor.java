/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance;

import software.wings.api.DeploymentInfo;

import java.util.List;
import java.util.Optional;

public interface DeploymentInfoExtractor {
  Optional<List<DeploymentInfo>> extractDeploymentInfo();
}
