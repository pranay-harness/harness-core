package software.wings.service.impl.workflow.creation.abstractfactories;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.exception.InvalidRequestException;
import software.wings.service.impl.workflow.creation.BuildWorkflowCreator;
import software.wings.service.impl.workflow.creation.CustomWorkflowCreator;
import software.wings.service.impl.workflow.creation.MultiPhaseWorkflowCreator;
import software.wings.service.impl.workflow.creation.SinglePhaseWorkflowCreator;
import software.wings.service.impl.workflow.creation.WorkflowCreator;
import software.wings.service.intfc.workflow.creation.WorkflowCreatorFactory;

@OwnedBy(CDC)
@Singleton
class GeneralWorkflowFactory implements WorkflowCreatorFactory {
  // For Canary/Multi Service
  @Inject private MultiPhaseWorkflowCreator multiPhaseWorkflowCreator;

  // For Basic/Rolling/BlueGreen
  @Inject private SinglePhaseWorkflowCreator singlePhaseWorkflowCreator;

  // For Build Workflow
  @Inject private BuildWorkflowCreator buildWorkflowCreator;

  // For Custom Workflow
  @Inject private CustomWorkflowCreator customWorkflowCreator;
  @Override
  public WorkflowCreator getWorkflowCreator(OrchestrationWorkflowType type) {
    switch (type) {
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
        unhandled(type);
        throw new InvalidRequestException("Unknown workflowType : " + type, USER);
    }
  }
}
