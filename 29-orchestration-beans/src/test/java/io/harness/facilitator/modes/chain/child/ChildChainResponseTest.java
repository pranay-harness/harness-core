package io.harness.facilitator.modes.chain.child;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansModuleListProvider;
import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitator.modes.chain.child.ChildChainResponse.ChildChainResponseBuilder;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationBeansModuleListProvider.class)
public class ChildChainResponseTest extends OrchestrationBeansTest {
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