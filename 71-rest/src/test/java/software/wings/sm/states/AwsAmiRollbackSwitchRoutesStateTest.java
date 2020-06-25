package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ARVIND;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContextImpl;

public class AwsAmiRollbackSwitchRoutesStateTest extends WingsBaseTest {
  @InjectMocks
  @Spy
  private final AwsAmiRollbackSwitchRoutesState state = new AwsAmiRollbackSwitchRoutesState("stateName");

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testExecute() throws InterruptedException {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    state.execute(mockContext);
    verify(state).executeInternal(any(), anyBoolean());
  }
}