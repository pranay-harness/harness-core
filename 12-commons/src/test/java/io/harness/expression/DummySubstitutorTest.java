package io.harness.expression;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DummySubstitutorTest {
  @Test
  public void shouldSubstitute() {
    assertThat(DummySubstitutor.substitute("http://user:${password}@host.com/index.php?var=${variable}"))
        .isEqualTo("http://user:dummy@host.com/index.php?var=dummy");
  }
}
