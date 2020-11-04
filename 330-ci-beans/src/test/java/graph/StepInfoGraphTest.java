package graph;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepInfoGraphTest extends CategoryTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void test_builder() {
    final StepInfoGraph stepInfoGraph = StepInfoGraph.builder().build();
    assertThat(stepInfoGraph).isNotNull();
    assertThat(stepInfoGraph.getAllNodes()).hasSize(0);
    assertThat(stepInfoGraph.getSteps()).isNull();
  }
}