package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContextImpl;

public class EcsBGRollbackRoute53DNSWeightStateTest extends WingsBaseTest {
  @Mock private EcsStateHelper ecsStateHelper;
  @InjectMocks
  @Spy
  private final EcsBGRollbackRoute53DNSWeightState state = new EcsBGRollbackRoute53DNSWeightState("stateName");

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testExecute() throws InterruptedException {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(null).when(state).executeInternal(any(), anyBoolean());
    state.execute(mockContext);
    verify(state).executeInternal(any(), anyBoolean());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(10).when(ecsStateHelper).getEcsStateTimeoutFromContext(anyObject(), anyBoolean());
    assertThat(state.getTimeoutMillis(mockContext)).isEqualTo(10);
  }
}