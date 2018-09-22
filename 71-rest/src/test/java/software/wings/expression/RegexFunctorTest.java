package software.wings.expression;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class RegexFunctorTest {
  @Test
  public void testExtract() {
    final RegexFunctor regexFunctor = new RegexFunctor();
    assertThat(regexFunctor.extract("[0-9]*", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
    assertThat(regexFunctor.extract("[0-9]+", "build-webservices-3935-0.noarch.rpm")).isEqualTo("3935");
  }
}
