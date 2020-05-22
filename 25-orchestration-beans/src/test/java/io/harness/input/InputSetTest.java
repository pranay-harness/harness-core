package io.harness.input;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
    return ImmutableMap.of("a", "va", "b.c.d", "vd", "b.c.e", "ve", "b.f", "vf");
  }
}