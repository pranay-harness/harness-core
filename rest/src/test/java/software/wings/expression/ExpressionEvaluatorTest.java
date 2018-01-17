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
  }

  @Test
  public void shouldEvaluateHostUrl() {
    Host host = new Host();
    host.setHostName("app123.application.com");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);

    String retValue = expressionEvaluator.substitute("http://${host.hostName}:8080/health/status", context);
    assertThat(retValue).isEqualTo("http://app123.application.com:8080/health/status");
  }

  @Test
  public void shouldEvaluatePartially() {
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);
    String retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context);
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
      }
    };
    String retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context);
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.io:${PORT}/health/status");
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
        .isInstanceOf(IllegalStateException.class);
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
    assertThat(expressionEvaluator.substitute("${re.extract('has matching', 'text has matching pattern')}", persons))
        .isEqualTo("has matching");

    assertThat(expressionEvaluator.substitute("${re.extract('has matching', 'no matching pattern')}", persons))
        .isEqualTo("");

    assertThat(expressionEvaluator.substitute("${re.extract('Y..k', '${bob.address.city}')}", persons))
        .isEqualTo("York");
  }

  @Test
  public void shouldSubstituteReReplace() {
    assertThat(expressionEvaluator.substitute("${re.replace('foo', 'bar', 'foo bar baz')}", persons))
        .isEqualTo("bar bar baz");

    assertThat(expressionEvaluator.substitute("${re.replace('foo', '${bob.address.city}', 'foo bar baz')}", persons))
        .isEqualTo("New York bar baz");

    assertThat(
        expressionEvaluator.substitute("${re.replace('.*(York)', 'New $1, New $1', '${bob.address.city}')}", persons))
        .isEqualTo("New York, New York");
  }

  @Test
  public void shouldSubstituteReMatch() {
    assertThat(expressionEvaluator.evaluate("re.match('has matching', 'text has matching pattern')", persons))
        .isEqualTo(true);

    assertThat(expressionEvaluator.evaluate("re.match('.*has matching.*', 'text has matching pattern')", persons))
        .isEqualTo(true);

    assertThat(expressionEvaluator.evaluate("re.match('^has matching$', 'text has matching pattern')", persons))
        .isEqualTo(false);

    assertThat(expressionEvaluator.evaluate("re.match('has matching', 'no matching pattern')", persons))
        .isEqualTo(false);

    assertThat(expressionEvaluator.evaluate("re.match('York', ${bob.address.city})", persons)).isEqualTo(true);
  }
}
