package software.wings.beans;

import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;

public interface LoopParams {
  State getEnvStateInstanceFromParams(WorkflowService workflowService, String appId);
}
