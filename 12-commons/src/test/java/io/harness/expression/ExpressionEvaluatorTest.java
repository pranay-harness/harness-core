package io.harness.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ExpressionEvaluatorTest extends CategoryTest {
  @Builder
  @Value
  public static class Person {
    private Address address;
    private int age;
  }

  @Builder
  @Value
  public static class Address {
    private String city;
  }

  Person sam = Person.builder().age(20).address(Address.builder().city("San Francisco").build()).build();
  Person bob = Person.builder().age(40).address(Address.builder().city("New York").build()).build();

  Map<String, Object> persons = new HashMap<String, Object>() {
    {
      put("sam", sam);
      put("bob", bob);
    }
  };

  @Test
  public void testNormalizeExpression() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    assertThat(expressionEvaluator.normalizeExpression("address.city.length()", persons, "bob"))
        .isEqualTo("address.city.length()");
    assertThat(expressionEvaluator.normalizeExpression("${sam.address.city.length()}", persons, "bob"))
        .isEqualTo("sam.address.city.length()");
    assertThat(expressionEvaluator.normalizeExpression("${address.city.length()}", persons, "bob"))
        .isEqualTo("bob.address.city.length()");

    assertThat(expressionEvaluator.normalizeExpression("${foo}", persons, "bar")).isEqualTo("foo");
  }

  @Test
  public void shouldSubstituteWithNull() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    assertThat(expressionEvaluator.substitute(null, persons)).isNull();
    assertThat(expressionEvaluator.substitute(null, persons, null)).isNull();
  }

  @Test
  public void shouldEvaluateRecursively() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    Map<String, Object> context = ImmutableMap.<String, Object>builder().put("name", "bob").put("bob", bob).build();
    assertThat(expressionEvaluator.substitute("${${name}.address.city}", context)).isEqualTo("New York");
  }

  @Test
  public void shouldEvaluateVar() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                      .put("vare", ImmutableMap.<String, Object>builder().put("foo", "bar").build())
                                      .build();
    String retValue = expressionEvaluator.substitute("--- ${vare.foo} ---", context);
    assertThat(retValue).isEqualTo("--- bar ---");
  }

  @Test
  public void shouldEvaluateWithNull() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    assertThat(expressionEvaluator.evaluate(null, persons)).isNull();
    assertThat(expressionEvaluator.evaluate(null, persons, null)).isNull();
  }

  @Test
  public void shouldEvaluateWithNameValue() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    String expr = "sam.age < 25 && sam.address.city=='San Francisco'";
    Object retValue = expressionEvaluator.evaluate(expr, "sam", sam);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);

    expr = "sam.getAge() == 20 && sam.getAddress().city.length()==13";
    retValue = expressionEvaluator.evaluate(expr, "sam", sam);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);

    expr = "sam.address.city";
    retValue = expressionEvaluator.evaluate(expr, "sam", sam);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(String.class);
    assertThat(retValue).isEqualTo("San Francisco");
  }

  @Test
  public void shouldEvaluateWithMap() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    String expr = "sam.age < bob.age && sam.address.city.length()>bob.address.city.length()";
    Object retValue = expressionEvaluator.evaluate(expr, persons);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  @Test
  public void shouldEvaluateWithDefaultPrefix() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    String expr = "sam.age < bob.age && sam.address.city.length() > ${address.city.length()}";
    Object retValue = expressionEvaluator.evaluate(expr, persons, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  @Test
  public void shouldSubstituteWithDefaultPrefix() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    String expr = "${sam.address.city}, ${address.city}";
    Object retValue = expressionEvaluator.substitute(expr, persons, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isEqualTo("San Francisco, New York");
  }

  @Test
  public void testNormalizeExpressionDoNotExpandRegex() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    assertThat(expressionEvaluator.normalizeExpression("${regex.match('', '')}", persons, "bob"))
        .isEqualTo("regex.match('', '')");
  }

  @Test
  public void shouldSubstituteReExtract() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    assertThat(expressionEvaluator.substitute("${regex.extract('Y..k', ${bob.address.city})}", persons))
        .isEqualTo("York");

    assertThat(expressionEvaluator.substitute("${regex.extract('..', ${bob.address.city})}", persons)).isEqualTo("Ne");

    assertThat(expressionEvaluator.substitute("${regex.extract('has matching', 'text has matching pattern')}", persons))
        .isEqualTo("has matching");

    assertThat(expressionEvaluator.substitute("${regex.extract('has matching', 'no matching pattern')}", persons))
        .isEqualTo("");
  }

  @Test
  public void shouldNotHangForCircle() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    Map<String, Object> context =
        ImmutableMap.<String, Object>builder().put("foo", "${bar}").put("bar", "${foo}").build();
    assertThatThrownBy(() -> expressionEvaluator.substitute("${foo}", context)).isInstanceOf(WingsException.class);
  }

  @Test
  public void testSubstituteDoNotExpandRe() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    assertThat(expressionEvaluator.substitute("${regex.extract('match', 'has matching pattern')}", persons, "bob"))
        .isEqualTo("match");
  }

  @Test
  public void shouldSubstituteReReplace() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    assertThat(expressionEvaluator.substitute("${regex.replace('foo', ${bob.address.city}, 'foo bar baz')}", persons))
        .isEqualTo("New York bar baz");

    assertThat(expressionEvaluator.substitute("${regex.replace('foo', 'bar', 'foo bar baz')}", persons))
        .isEqualTo("bar bar baz");

    assertThat(
        expressionEvaluator.substitute("${regex.replace('.*(York)', 'New $1, New $1', ${bob.address.city})}", persons))
        .isEqualTo("New York, New York");
  }

  @Test
  public void shouldSubstituteReMatch() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
    expressionEvaluator.addFunctor("regex", new RegexFunctor());

    assertThat(expressionEvaluator.evaluate("regex.match('has matching', 'text has matching pattern')", persons))
        .isEqualTo(true);

    assertThat(expressionEvaluator.evaluate("regex.match('.*has matching.*', 'text has matching pattern')", persons))
        .isEqualTo(true);

    assertThat(expressionEvaluator.evaluate("regex.match('^has matching$', 'text has matching pattern')", persons))
        .isEqualTo(false);

    assertThat(expressionEvaluator.evaluate("regex.match('has matching', 'no matching pattern')", persons))
        .isEqualTo(false);

    assertThat(expressionEvaluator.evaluate("regex.match('York', ${bob.address.city})", persons)).isEqualTo(true);
  }

  @Test
  public void shouldNotCollideVars() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                      .put("BA1BA", "${AC1AC}")
                                      .put("AC1AC", "${AB1AB}")
                                      .put("AB1AB", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                                      .build();
    assertThat(expressionEvaluator.substitute("${BA1BA}", context))
        .isEqualTo("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
  }

  @Test
  public void shouldBeOkWithSameVarsFromDifferentIterations() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context =
        ImmutableMap.<String, Object>builder().put("A", "${B}").put("B", "${C}").put("C", "done").build();
    assertThat(expressionEvaluator.substitute("${A}, ${B}", context)).isEqualTo("done, done");
  }

  @Test
  public void shouldDetectExponentialGrowth() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context =
        ImmutableMap.<String, Object>builder()
            .put("B", "${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A}")
            .put("A", "${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B}")
            .build();
    assertThatThrownBy(() -> expressionEvaluator.substitute("${A}", context)).isInstanceOf(WingsException.class);
  }

  @Test
  public void shouldRenderLateBoundValue() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context = new HashMap<>();

    context.put("workflow", new LateBindingValue() {
      @Override
      public Object bind() {
        return ImmutableMap.of("foo", "bar");
      }
    });

    assertThat(expressionEvaluator.substitute("${workflow.foo}", context)).isEqualTo("bar");
  }

  @Test
  public void shouldEscapeSpecialCharacters() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context =
        ImmutableMap.<String, Object>builder()
            .put("WINDOWS_RUNTIME_PATH", "%USERPROFILE%\\${app.name}\\${service.name}\\${env.name}\\runtime")
            .build();
    assertThat(expressionEvaluator.substitute("${WINDOWS_RUNTIME_PATH}", context))
        .isEqualTo("%USERPROFILE%\\${app.name}\\${service.name}\\${env.name}\\runtime");
  }
}
