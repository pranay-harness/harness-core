package io.harness.facilitator.modes.chain.child;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.chain.child.ChildChainResponse.ChildChainResponseBuilder;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ChildChainResponseTest extends OrchestrationBeansTestBase {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestBuild() {
    ChildChainResponseBuilder response = ChildChainResponse.builder().nextChildId(null).suspend(false);
    assertThatThrownBy(response::build)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("If not Suspended nextChildId cant be null");
  }
}
