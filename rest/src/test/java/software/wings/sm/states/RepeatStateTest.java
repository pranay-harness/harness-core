/**
 *
 */

package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ServiceElement;
import software.wings.beans.ExecutionStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class RepeatStateTest.
 *
 * @author Rishi
 */
public class RepeatStateTest {
  private static final Logger logger = LoggerFactory.getLogger(RepeatStateTest.class);

  /**
   * Should execute serial.
   */
  @Test
  public void shouldExecuteSerial() {
    String stateName = "test";

    List<ContextElement> repeatElements = getTestRepeatElements();

    ExecutionContextImpl context = prepareExecutionContext(stateName, repeatElements);
    RepeatState repeatState = new RepeatState(stateName);
    repeatState.setRepeatElementExpression("services()");
    repeatState.setExecutionStrategy(ExecutionStrategy.SERIAL);
    repeatState.setRepeatTransitionStateName("abc");
    repeatState.resolveProperties();

    assertThat(repeatState.getRepeatElementType()).isEqualTo(ContextElementType.SERVICE);

    ExecutionResponse response = repeatState.execute(context, null);

    assertResponse(repeatElements, response, 1);
    RepeatStateExecutionData stateExecutionData = (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElementIndex()).isNotNull();
    assertThat(stateExecutionData.getRepeatElementIndex()).isEqualTo(0);

    assertThat(response).isInstanceOf(SpawningExecutionResponse.class);
    assertThat(((SpawningExecutionResponse) response).getStateExecutionInstanceList()).isNotNull();
    assertThat(((SpawningExecutionResponse) response).getStateExecutionInstanceList().size()).isEqualTo(1);
  }

  /**
   * Should execute parallel.
   */
  @Test
  public void shouldExecuteParallel() {
    List<ContextElement> repeatElements = getTestRepeatElements();
    String stateName = "test";

    ExecutionContextImpl context = prepareExecutionContext(stateName, repeatElements);

    RepeatState repeatState = new RepeatState(stateName);
    repeatState.setRepeatElementExpression("services()");
    repeatState.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    repeatState.setRepeatTransitionStateName("abc");
    repeatState.resolveProperties();

    assertThat(repeatState.getRepeatElementType()).isEqualTo(ContextElementType.SERVICE);

    ExecutionResponse response = repeatState.execute(context, null);

    assertResponse(repeatElements, response, 2);
    RepeatStateExecutionData stateExecutionData = (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElementIndex()).isNull();

    assertThat(response).isInstanceOf(SpawningExecutionResponse.class);
    assertThat(((SpawningExecutionResponse) response).getStateExecutionInstanceList()).isNotNull();
    assertThat(((SpawningExecutionResponse) response).getStateExecutionInstanceList().size()).isEqualTo(2);
  }

  private void assertResponse(List<ContextElement> repeatElements, ExecutionResponse response, int corrIdsExpected) {
    assertThat(response).isNotNull();
    assertThat(response.isAsync()).as("Async Execution").isEqualTo(true);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData()).isInstanceOf(RepeatStateExecutionData.class);
    RepeatStateExecutionData stateExecutionData = (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElements()).isNotNull();
    assertThat(stateExecutionData.getRepeatElements()).isEqualTo(repeatElements);

    assertThat(response.getCorrelationIds()).isNotNull();
    assertThat(response.getCorrelationIds().size()).as("correlationIds").isEqualTo(corrIdsExpected);

    if (logger.isDebugEnabled()) {
      logger.debug("correlationIds: " + response.getCorrelationIds());
    }
  }

  private ExecutionContextImpl prepareExecutionContext(String stateName, List<ContextElement> repeatElements) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(generateUuid());
    stateExecutionInstance.setDisplayName(stateName);

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.evaluateExpression("services()")).thenReturn(repeatElements);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    return context;
  }

  private List<ContextElement> getTestRepeatElements() {
    List<ContextElement> repeatElements = new ArrayList<>();
    ServiceElement ui = new ServiceElement();
    ui.setName("ui");
    repeatElements.add(ui);

    ServiceElement svr = new ServiceElement();
    svr.setName("server");
    repeatElements.add(svr);
    return repeatElements;
  }
}
