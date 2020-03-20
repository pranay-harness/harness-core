package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class EcsServiceRollbackTest extends WingsBaseTest {
  @InjectMocks private EcsServiceRollback ecsServiceRollback = new EcsServiceRollback("stateName");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithNullRollbackElement() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.getContextElement(
             ContextElementType.PARAM, ContainerRollbackRequestElement.CONTAINER_ROLLBACK_REQUEST_PARAM))
        .thenReturn(null);
    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }
}
