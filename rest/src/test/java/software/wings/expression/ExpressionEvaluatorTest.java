/**
 *
 */

package software.wings.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.Host;
import software.wings.exception.WingsException;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ExpressionEvaluatorTest.
 *
 * @author Rishi
 */
public class ExpressionEvaluatorTest extends WingsBaseTest {
  @Inject private ExpressionEvaluator expressionEvaluator;

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
    assertThat(expressionEvaluator.normalizeExpression("address.city.length()", persons, "bob"))
        .isEqualTo("address.city.length()");
    assertThat(expressionEvaluator.normalizeExpression("${sam.address.city.length()}", persons, "bob"))
        .isEqualTo("sam.address.city.length()");
    assertThat(expressionEvaluator.normalizeExpression("${address.city.length()}", persons, "bob"))
        .isEqualTo("bob.address.city.length()");

    assertThat(expressionEvaluator.normalizeExpression("${foo}", persons, "bar")).isEqualTo("foo");
  }

  @Test
  public void testNormalizeExpressionDoNotExpandRe() {
    assertThat(expressionEvaluator.normalizeExpression("${regex.match('', '')}", persons, "bob"))
        .isEqualTo("regex.match('', '')");
  }

  @Test
  public void shouldSubstituteWithNull() {
    assertThat(expressionEvaluator.substitute(null, persons)).isNull();
    assertThat(expressionEvaluator.substitute(null, persons, null)).isNull();
  }

  @Test
  public void shouldSubstituteHostUrl() {
    Host host = new Host();
    host.setHostName("app123.application.com");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);

    String retValue = expressionEvaluator.substitute("http://${host.hostName}:8080/health/status", context);
    assertThat(retValue).isEqualTo("http://app123.application.com:8080/health/status");
  }

  @Test
  public void shouldSubstitutePartially() {
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);
    String retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context);
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.${COM}:${PORT}/health/status");

    retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context, "bar");
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.${COM}:${PORT}/health/status");
  }

  @Test
  public void shouldEvaluateRecursively() {
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<String, Object>() {
      {
        put("host", host);
        put("COM", "io");
        put("name", "bob");
        put("bob", bob);
      }
    };
    String retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context);
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.io:${PORT}/health/status");

    assertThat(expressionEvaluator.substitute("${${name}.address.city}", context)).isEqualTo("New York");
  }

  @Test
  public void shouldNotHangForCircle() {
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<String, Object>() {
      {
        put("host", host);
        put("COM", "${host.hostName}");
      }
    };
    assertThatThrownBy(() -> expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context))
        .isInstanceOf(WingsException.class);
  }

  @Test
  public void shouldEvaluateWithNull() {
    assertThat(expressionEvaluator.evaluate(null, persons)).isNull();
    assertThat(expressionEvaluator.evaluate(null, persons, null)).isNull();
  }

  @Test
  public void shouldEvaluateWithNameValue() {
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
    String expr = "sam.age < bob.age && sam.address.city.length()>bob.address.city.length()";
    Object retValue = expressionEvaluator.evaluate(expr, persons);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  @Test
  public void shouldEvaluateWithDefaultPrefix() {
    String expr = "sam.age < bob.age && sam.address.city.length() > ${address.city.length()}";
    Object retValue = expressionEvaluator.evaluate(expr, persons, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  @Test
  public void shouldSubstituteWithDefaultPrefix() {
    String expr = "${sam.address.city}, ${address.city}";
    Object retValue = expressionEvaluator.substitute(expr, persons, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isEqualTo("San Francisco, New York");
  }

  @Test
  public void shouldSubstituteReExtract() {
    assertThat(expressionEvaluator.substitute("${regex.extract('Y..k', ${bob.address.city})}", persons))
        .isEqualTo("York");

    assertThat(expressionEvaluator.substitute("${regex.extract('has matching', 'text has matching pattern')}", persons))
        .isEqualTo("has matching");

    assertThat(expressionEvaluator.substitute("${regex.extract('has matching', 'no matching pattern')}", persons))
        .isEqualTo("");
  }

  @Test
  public void testSubstituteDoNotExpandRe() {
    assertThat(expressionEvaluator.substitute("${regex.extract('match', 'has matching pattern')}", persons, "bob"))
        .isEqualTo("match");
  }

  @Test
  public void shouldSubstituteReReplace() {
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
    Map<String, Object> context = new HashMap<String, Object>() {
      {
        put("BA1BA", "${AC1AC}");
        put("AC1AC", "${AB1AB}");
        put("AB1AB", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
      }
    };
    assertThat(expressionEvaluator.substitute("${BA1BA}", context))
        .isEqualTo("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
  }

  @Test
  public void shouldBeOkWithSameVarsFromDifferentIterations() {
    Map<String, Object> context = new HashMap<String, Object>() {
      {
        put("A", "${B}");
        put("B", "${C}");
        put("C", "done");
      }
    };
    assertThat(expressionEvaluator.substitute("${A}, ${B}", context)).isEqualTo("done, done");
  }

  @Test
  public void shouldDetectExponentialGrowth() {
    Map<String, Object> context = new HashMap<String, Object>() {
      {
        put("B", "${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A} ${A}");
        put("A", "${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B} ${B}");
      }
    };
    assertThatThrownBy(() -> expressionEvaluator.substitute("${A}", context)).isInstanceOf(WingsException.class);
  }
}
