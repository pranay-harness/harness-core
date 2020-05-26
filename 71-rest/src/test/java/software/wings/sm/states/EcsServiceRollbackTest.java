package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class EcsServiceRollbackTest extends WingsBaseTest {
  @Mock private EcsStateHelper mockEcsStateHelper;

  @InjectMocks private EcsServiceRollback ecsServiceRollback = new EcsServiceRollback("stateName");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithNullRollbackElement() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(null).when(mockEcsStateHelper).getDeployElementFromSweepingOutput(any());
    ExecutionResponse response = ecsServiceRollback.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }
}
