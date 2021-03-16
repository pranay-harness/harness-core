package io.harness.text;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StringReplacerTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testStringReplacer() {
    String source = "abc def <+gh <+ij <+kl> > > <+mn>>> op";
    String expected = "abc def 1 2>> op";
    DummyExpressionResolver resolver = new DummyExpressionResolver();
    StringReplacer stringReplacer = new StringReplacer(resolver, "<+", ">");
    String resp = stringReplacer.replace(source);
    assertThat(resp).isNotNull();
    assertThat(resp).isEqualTo(expected);

    List<String> expressions = resolver.getExpressions();
    assertThat(expressions).isNotEmpty();
    assertThat(expressions).containsExactly("gh <+ij <+kl> > ", "mn");
  }
}
