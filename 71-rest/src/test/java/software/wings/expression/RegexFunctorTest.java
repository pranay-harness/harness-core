package software.wings.expression;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.expression.RegexFunctor;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RegexFunctorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testExtract() {
    final RegexFunctor regexFunctor = new RegexFunctor();
    assertThat(regexFunctor.extract("[0-9]*", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
    assertThat(regexFunctor.extract("[0-9]+", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
  }
}
