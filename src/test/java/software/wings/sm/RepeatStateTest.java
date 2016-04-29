/**
 *
 */

package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.common.UUIDGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rishi
 *
 */
public class RepeatStateTest {
  @Test
  public void shouldExecute() {
    StateMachineExecutor stateMachineExecutor = Mockito.mock(StateMachineExecutor.class);
    // ExecutionContext context = Mockito.mock(ExecutionContext.class);
    ExecutionContext context = new ExecutionContext();
    context = spy(context);
    List<RepeatElement> repeatElements = new ArrayList<>();

    ServiceElement ui = new ServiceElement();
    Service svc = new Service();
    svc.setName("ui");
    ui.setService(svc);
    repeatElements.add(ui);

    ServiceElement svr = new ServiceElement();
    svc = new Service();
    svc.setName("server");
    svr.setService(svc);
    repeatElements.add(svr);

    when(context.evaluateRepeatExpression(RepeatElementType.SERVICE, "services()")).thenReturn(repeatElements);

    SmInstance smInstance = new SmInstance();
    smInstance.setUuid(UUIDGenerator.getUuid());

    when(context.getSmInstance()).thenReturn(smInstance);

    RepeatState repeatState = new RepeatState("test");

    repeatState.setRepeatElementExpression("services()");
    repeatState.setRepeatElementType(RepeatElementType.SERVICE);

    ExecutionResponse response = repeatState.execute(context, stateMachineExecutor);

    assertThat(response).isNotNull();
    assertThat(response.isAsynch()).as("Asynch Execution").isEqualTo(true);
    assertThat(context.getRepeatElement(RepeatElementType.SERVICE)).isNotNull();
    assertThat(context.getRepeatElement(RepeatElementType.SERVICE)).as("First repeat elements").isEqualTo(ui);
    assertThat(response.getCorrelationIds()).isNotNull();
    assertThat(response.getCorrelationIds().size()).as("correlationIds").isEqualTo(1);
    assertThat(context.getParams().get(RepeatState.REPEAT_ELEMENT_INDEX)).as("correlationIds").isEqualTo(0);

    logger.debug("correlationIds: " + response.getCorrelationIds());
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
}
