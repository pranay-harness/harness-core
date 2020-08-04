package software.wings.sm.states;

import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.EcsSetupElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.states.utils.StateTimeoutUtils;

public class StateTimeoutUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(null).when(mockContext).getContextElement(ContextElementType.ECS_SERVICE_SETUP);
    assertThat(StateTimeoutUtils.getEcsStateTimeoutFromContext(mockContext)).isEqualTo(null);

    doReturn(EcsSetupElement.builder().serviceSteadyStateTimeout(10).build())
        .when(mockContext)
        .getContextElement(ContextElementType.ECS_SERVICE_SETUP);
    assertThat(StateTimeoutUtils.getEcsStateTimeoutFromContext(mockContext)).isEqualTo(600000);

    doReturn(EcsSetupElement.builder().serviceSteadyStateTimeout(0).build())
        .when(mockContext)
        .getContextElement(ContextElementType.ECS_SERVICE_SETUP);
    assertThat(StateTimeoutUtils.getEcsStateTimeoutFromContext(mockContext)).isEqualTo(null);

    doReturn(EcsSetupElement.builder().serviceSteadyStateTimeout(null).build())
        .when(mockContext)
        .getContextElement(ContextElementType.ECS_SERVICE_SETUP);
    assertThat(StateTimeoutUtils.getEcsStateTimeoutFromContext(mockContext)).isEqualTo(null);
  }
}