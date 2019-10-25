package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.sm.ExecutionContextImpl;

public class ResourceConstraintStateTest extends WingsBaseTest {
  @Mock ExecutionContextImpl executionContext;
  @Mock ResourceConstraintService resourceConstraintService;
  @InjectMocks ResourceConstraintState state = new ResourceConstraintState("rcs");

  private String phaseName;

  @Before
  public void setUp() {
    phaseName = "phase-name";
    Application app = Application.Builder.anApplication().uuid(APP_ID).accountId(ACCOUNT_ID).build();
    when(executionContext.fetchRequiredApp()).thenReturn(app);
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM))
        .thenReturn(PhaseElement.builder().phaseName(phaseName).build());
  }

  @Test
  @Category(UnitTests.class)
  public void alreadyAcquiredPermits() {
    when(resourceConstraintService.getAllCurrentlyAcquiredPermits(
             HoldingScope.WORKFLOW.name(), ResourceConstraintService.releaseEntityId(WORKFLOW_EXECUTION_ID), APP_ID))
        .thenReturn(0, 1);
    when(resourceConstraintService.getAllCurrentlyAcquiredPermits(HoldingScope.PHASE.name(),
             ResourceConstraintService.releaseEntityId(WORKFLOW_EXECUTION_ID, phaseName), APP_ID))
        .thenReturn(0, 1);
    int permits_1 = state.alreadyAcquiredPermits(HoldingScope.WORKFLOW.name(), executionContext);
    int permits_2 = state.alreadyAcquiredPermits(HoldingScope.WORKFLOW.name(), executionContext);
    assertThat(permits_1).isEqualTo(0);
    assertThat(permits_2).isEqualTo(1);

    int permits_3 = state.alreadyAcquiredPermits(HoldingScope.PHASE.name(), executionContext);
    int permits_4 = state.alreadyAcquiredPermits(HoldingScope.PHASE.name(), executionContext);
    assertThat(permits_3).isEqualTo(1);
    assertThat(permits_4).isEqualTo(2);
  }
}