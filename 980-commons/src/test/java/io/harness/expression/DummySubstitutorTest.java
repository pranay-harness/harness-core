package io.harness.expression;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DummySubstitutorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSubstitute() {
    assertThat(DummySubstitutor.substitute("http://user:${password}@host.com/index?var=${variable}"))
        .isEqualTo("http://user:CD36671D4E034D3E8732217BD43F9AFA@host.com/index?var=CD36671D4E034D3E8732217BD43F9AFA");
  }
}
