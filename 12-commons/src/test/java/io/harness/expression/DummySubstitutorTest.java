package io.harness.expression;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DummySubstitutorTest {
  @Test
  @Category(UnitTests.class)
  public void shouldSubstitute() {
    assertThat(DummySubstitutor.substitute("http://user:${password}@host.com/index.php?var=${variable}"))
        .isEqualTo("http://user:dummy@host.com/index.php?var=dummy");
  }
}
