/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;

public interface LoopParams {
  State getEnvStateInstanceFromParams(WorkflowService workflowService, String appId);
}
