package software.wings.service.impl.template;

import static java.util.Arrays.asList;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import org.junit.Before;
import software.wings.WingsBaseTest;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.template.Template;
import software.wings.common.Constants;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

public class TemplateBaseTest extends WingsBaseTest {
  @Inject protected TemplateFolderService templateFolderService;
  @Inject protected TemplateService templateService;
  @Inject protected TemplateGalleryService templateGalleryService;

  @Before
  public void setUp() {
    templateGalleryService.loadHarnessGallery();
  }

  protected Workflow generateWorkflow(Template savedTemplate, GraphNode step) {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withUuid(WORKFLOW_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).addStep(step).build())
                .addWorkflowPhase(
                    aWorkflowPhase()
                        .infraMappingId(INFRA_MAPPING_ID)
                        .serviceId(SERVICE_ID)
                        .deploymentType(SSH)
                        .phaseSteps(asList(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE).addStep(step).build()))
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(step).build())
                .build())
        .withLinkedTemplateUuids(asList(savedTemplate.getUuid()))
        .build();
  }
}
