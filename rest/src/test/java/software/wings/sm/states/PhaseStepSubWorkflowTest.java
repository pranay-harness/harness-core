package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.common.UUIDGenerator.getUuid;

import org.junit.Test;
import software.wings.api.PhaseElement;
import software.wings.beans.ErrorCode;
import software.wings.beans.PhaseStepType;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;

/**
 * Created by rishi on 2/25/17.
 */
public class PhaseStepSubWorkflowTest {
  @Test
  public void shouldExecuteContainerDeployStep() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    PhaseElement phaseElement =
        aPhaseElement()
            .withUuid(getUuid())
            .withServiceElement(aServiceElement().withUuid(getUuid()).withName("service1").build())
            .build();
    stateExecutionInstance.getContextElements().add(phaseElement);
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("step1");
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.PRE_DEPLOYMENT);
    ExecutionResponse response = phaseStepSubWorkflow.execute(context);
    assertThat(response).isNotNull();
  }

  @Test
  public void shouldThrowNullPhaseType() {
    try {
      ExecutionContextImpl context = new ExecutionContextImpl(null);
      PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("step1");
      ExecutionResponse response = phaseStepSubWorkflow.execute(context);
      assertThat(response).isNotNull();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("null phaseStepType");
    }
  }
}
