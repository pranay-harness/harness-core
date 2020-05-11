package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class WorkflowCreatorFactory {
  // For Kubernetes V2
  @Inject private K8V2CanaryWorkflowCreator k8V2CanaryWorkflowCreator;
  @Inject private K8V2RollingWorkflowCreator k8V2RollingWorkflowCreator;
  @Inject private K8V2BlueGreenWorkflowCreator k8V2BlueGreenWorkflowCreator;

  // For Canary/Multi Service
  @Inject private MultiPhaseWorkflowCreator multiPhaseWorkflowCreator;

  // For Basic/Rolling/BlueGreen
  @Inject private SinglePhaseWorkflowCreator singlePhaseWorkflowCreator;

  // For Build Workflow
  @Inject private BuildWorkflowCreator buildWorkflowCreator;

  // For Custom Workflow
  @Inject private CustomWorkflowCreator customWorkflowCreator;

  public WorkflowCreator getWorkflowCreatorFactory(
      OrchestrationWorkflowType orchestrationWorkflowType, boolean isV2ServicePresent) {
    if (isV2ServicePresent) {
      switch (orchestrationWorkflowType) {
        case CANARY:
        case MULTI_SERVICE:
          return k8V2CanaryWorkflowCreator;
        case BLUE_GREEN:
          return k8V2BlueGreenWorkflowCreator;
        case ROLLING:
          return k8V2RollingWorkflowCreator;
        default:
          throw new InvalidRequestException(
              String.format("WorkflowType %s not supported for Kubernetes V2", orchestrationWorkflowType), USER);
      }
    } else {
      switch (orchestrationWorkflowType) {
        case BASIC:
        case ROLLING:
        case BLUE_GREEN:
          return singlePhaseWorkflowCreator;
        case CANARY:
        case MULTI_SERVICE:
          return multiPhaseWorkflowCreator;
        case BUILD:
          return buildWorkflowCreator;
        case CUSTOM:
          return customWorkflowCreator;
        default:
          unhandled(orchestrationWorkflowType);
          throw new InvalidRequestException("Unknown workflowType : " + orchestrationWorkflowType, USER);
      }
    }
  }
}
