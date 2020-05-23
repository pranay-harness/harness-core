package io.harness.plan.input;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.DummyOutcome;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class InputSetTest extends OrchestrationBeansTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testWithOverridesForMap() {
    InputSet inputSet = new InputSet().withOverrides(prepareMap());
    validateInputSet(inputSet);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testWithOverridesForInputSet() {
    InputSet inputSet = new InputSet().withOverrides(prepareMap());
    InputSet inputSetNew = new InputSet().withOverrides(inputSet);
    validateInputSet(inputSetNew);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPutForInputSet() {
    InputSet inputSet = new InputSet();
    inputSet.put("a.b", "vb");
    inputSet.put("a.d", ImmutableMap.of("e", "f", "g", "h"));
    inputSet.put("a.dummy", new DummyOutcome());
    assertThat(inputSet.get("a.b")).isEqualTo("vb");
    assertThat(inputSet.get("a.d.e")).isEqualTo("f");
    assertThat(inputSet.get("a.d.g")).isEqualTo("h");
    assertThat(inputSet.get("a.dummy")).isInstanceOf(DummyOutcome.class);
  }

  private void validateInputSet(InputSet inputSet) {
    assertThat(inputSet.get("a")).isEqualTo("va");
    assertThat(inputSet.get("b.c.d")).isEqualTo("vd");
    assertThat(inputSet.get("b.c.e")).isEqualTo("ve");
    assertThat(inputSet.get("b.f")).isEqualTo("vf");
    assertThat(inputSet.get("k")).isNull();
    assertThat(inputSet.get("b.g")).isNull();
    assertThat(inputSet.get("b.c.k")).isNull();
  }

  private Map<String, Object> prepareMap() {
    return ImmutableMap.<String, Object>builder()
        .put("a", "va")
        .put("b",
            ImmutableMap.<String, Object>builder()
                .put("c", ImmutableMap.of("d", "vd", "e", "ve"))
                .put("f", "vf")
                .build())
        .build();
  }
}