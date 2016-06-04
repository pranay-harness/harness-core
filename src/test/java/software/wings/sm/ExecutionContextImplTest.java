/**
 *
 */
package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.api.ServiceElement;
import software.wings.common.UUIDGenerator;

// TODO: Auto-generated Javadoc

/**
 * The Class ExecutionContextImplTest.
 *
 * @author Rishi
 */
public class ExecutionContextImplTest {
  /**
   * Should fetch context element.
   */
  @Test
  public void shouldFetchContextElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();

    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

    ServiceElement element1 = new ServiceElement();
    element1.setUuid(UUIDGenerator.getUuid());
    element1.setName("svc1");
    context.pushContextElement(element1);

    ServiceElement element2 = new ServiceElement();
    element2.setUuid(UUIDGenerator.getUuid());
    element2.setName("svc2");
    context.pushContextElement(element2);

    ServiceElement element3 = new ServiceElement();
    element3.setUuid(UUIDGenerator.getUuid());
    element3.setName("svc3");
    context.pushContextElement(element3);

    ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
    assertThat(element).isNotNull();
    assertThat(element).isEqualToComparingFieldByField(element3);
  }
}
