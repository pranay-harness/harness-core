package software.wings.expression;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.expression.RegexFunctor;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RegexFunctorTest {
  @Test
  @Category(UnitTests.class)
  public void testExtract() {
    final RegexFunctor regexFunctor = new RegexFunctor();
    assertThat(regexFunctor.extract("[0-9]*", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
    assertThat(regexFunctor.extract("[0-9]+", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
  }
}
