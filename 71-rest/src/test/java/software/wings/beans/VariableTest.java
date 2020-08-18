package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Variable.VariableBuilder.aVariable;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.Arrays;

public class VariableTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void but() {
    Variable src = aVariable()
                       .name("var")
                       .value("foo")
                       .type(VariableType.TEXT)
                       .description("desc")
                       .mandatory(true)
                       .metadata(ImmutableMap.of("key", "value"))
                       .allowMultipleValues(true)
                       .allowedValues("foo, bar")
                       .allowedList(Arrays.asList("a", "b"))
                       .build();

    Variable dst = src.but().value("new-value").build();

    assertThat(dst).isEqualToIgnoringGivenFields(src, "value");
    assertThat(dst.getValue()).isEqualTo("new-value");
  }
}