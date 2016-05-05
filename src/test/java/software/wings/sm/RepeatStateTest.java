/**
 *
 */

package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.common.UUIDGenerator;
import software.wings.sm.RepeatState.RepeatStateExecutionData;
import software.wings.sm.RepeatState.RepeatStrategy;
import software.wings.utils.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rishi
 *
 */
public class RepeatStateTest {
  @Test
  public void shouldExecuteSerial() {
    List<Repeatable> repeatElements = getTestRepeatElements();
    String stateName = "test";

    ExpressionEvaluator evaluator = Mockito.mock(ExpressionEvaluator.class);
    when(evaluator.evaluate("services()", null)).thenReturn(repeatElements);

    ExecutionContextImpl context = prepareExecutionContext(stateName, evaluator);

    RepeatState repeatState = new RepeatState(stateName);
    repeatState.setRepeatElementExpression("services()");
    repeatState.setRepeatElementType(RepeatElementType.SERVICE);
    repeatState.setRepeatStrategy(RepeatStrategy.SERIAL);

    AtomicInteger counter = new AtomicInteger(0);
    StateMachineExecutor stateMachineExecutor = getStateMachineExecutorMock(counter);

    ExecutionResponse response = repeatState.execute(context, null, stateMachineExecutor);

    assertResponse(repeatElements, response, 1);
    RepeatState.RepeatStateExecutionData stateExecutionData =
        (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElementIndex()).isNotNull();
    assertThat(stateExecutionData.getRepeatElementIndex()).isEqualTo(0);

    assertThat(counter.get()).as("No of Executions").isEqualTo(1);
  }

  @Test
  public void shouldExecuteParallel() {
    List<Repeatable> repeatElements = getTestRepeatElements();
    String stateName = "test";

    ExpressionEvaluator evaluator = Mockito.mock(ExpressionEvaluator.class);
    when(evaluator.evaluate("services()", null)).thenReturn(repeatElements);

    ExecutionContextImpl context = prepareExecutionContext(stateName, evaluator);

    RepeatState repeatState = new RepeatState(stateName);
    repeatState.setRepeatElementExpression("services()");
    repeatState.setRepeatElementType(RepeatElementType.SERVICE);
    repeatState.setRepeatStrategy(RepeatStrategy.PARALLEL);

    AtomicInteger counter = new AtomicInteger(0);
    StateMachineExecutor stateMachineExecutor = getStateMachineExecutorMock(counter);

    ExecutionResponse response = repeatState.execute(context, null, stateMachineExecutor);

    assertResponse(repeatElements, response, 2);
    RepeatState.RepeatStateExecutionData stateExecutionData =
        (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElementIndex()).isNull();

    assertThat(counter.get()).as("No of Executions").isEqualTo(2);
  }

  private StateMachineExecutor getStateMachineExecutorMock(AtomicInteger counter) {
    StateMachineExecutor stateMachineExecutor = Mockito.mock(StateMachineExecutor.class);
    Mockito
        .doAnswer(new Answer() {

          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            counter.incrementAndGet();
            return null;
          }

        })
        .when(stateMachineExecutor)
        .execute(anyString(), anyString(), any(ExecutionContextImpl.class), anyString(), anyString());
    return stateMachineExecutor;
  }

  private void assertResponse(List<Repeatable> repeatElements, ExecutionResponse response, int corrIdsExpected) {
    assertThat(response).isNotNull();
    assertThat(response.isAsynch()).as("Asynch Execution").isEqualTo(true);
    assertThat(response.getStateExecutionData()).isNotNull();
    assertThat(response.getStateExecutionData()).isInstanceOf(RepeatState.RepeatStateExecutionData.class);
    RepeatState.RepeatStateExecutionData stateExecutionData =
        (RepeatStateExecutionData) response.getStateExecutionData();

    assertThat(stateExecutionData.getRepeatElements()).isNotNull();
    assertThat(stateExecutionData.getRepeatElements()).isEqualTo(repeatElements);

    assertThat(response.getCorrelationIds()).isNotNull();
    assertThat(response.getCorrelationIds().size()).as("correlationIds").isEqualTo(corrIdsExpected);

    logger.debug("correlationIds: " + response.getCorrelationIds());
  }

  private ExecutionContextImpl prepareExecutionContext(String stateName, ExpressionEvaluator evaluator) {
    ExecutionContextImpl context = new ExecutionContextImpl();
    SmInstance smInstance = new SmInstance();
    smInstance.setUuid(UUIDGenerator.getUuid());
    smInstance.setStateName(stateName);
    context.setSmInstance(smInstance);

    context.setEvaluator(evaluator);
    return context;
  }

  private List<Repeatable> getTestRepeatElements() {
    List<Repeatable> repeatElements = new ArrayList<>();
    Service ui = new Service();
    ui.setName("ui");
    repeatElements.add(ui);

    Service svr = new Service();
    svr.setName("server");
    repeatElements.add(svr);
    return repeatElements;
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
}
