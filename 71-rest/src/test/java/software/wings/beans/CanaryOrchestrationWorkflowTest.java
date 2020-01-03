package software.wings.beans;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class CanaryOrchestrationWorkflowTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void checkLastPhaseForOnDemandRollback() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .addWorkflowPhase(aWorkflowPhase()
                                  .name("Phase 1")
                                  .infraMappingId(INFRA_MAPPING_ID)
                                  .serviceId(SERVICE_ID)
                                  .deploymentType(SSH)
                                  .build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 1")).isTrue();
    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 2")).isFalse();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void checkLastPhaseForOnDemandRollbackNoPhases() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow()
            .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
            .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
            .build();

    assertThat(canaryOrchestrationWorkflow.checkLastPhaseForOnDemandRollback("Staging Execution Phase 1")).isFalse();
  }
}