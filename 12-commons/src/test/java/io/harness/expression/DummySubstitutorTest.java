package io.harness.expression;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DummySubstitutorTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldSubstitute() {
    assertThat(DummySubstitutor.substitute("http://user:${password}@host.com/index.php?var=${variable}"))
        .isEqualTo("http://user:dummy@host.com/index.php?var=dummy");
  }
}
