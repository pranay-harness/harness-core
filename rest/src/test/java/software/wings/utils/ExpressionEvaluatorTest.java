/**
 *
 */

package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

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

  /**
   * Should evaluate host url.
   */
  @Test
  public void shouldEvaluateHostUrl() {
    String expression = "http://${host.hostName}:8080/health/status";
    Host host = new Host();
    host.setHostName("app123.application.com");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);
    String retValue = expressionEvaluator.merge(expression, context);
    assertThat(retValue).isEqualTo("http://app123.application.com:8080/health/status");
  }

  /**
   * Should evaluate host url.
   */
  @Test
  public void shouldEvaluatePartially() {
    String expression = "http://${host.hostName}:${PORT}/health/status";
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);
    String retValue = expressionEvaluator.merge(expression, context);
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.${COM}:${PORT}/health/status");
  }

  /**
   * Should evaluate with name value.
   */
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

  /**
   * Should evaluate with map.
   */
  @Test
  public void shouldEvaluateWithMap() {
    String expr = "sam.age < bob.age && sam.address.city.length()>bob.address.city.length()";
    Map<String, Object> map = new HashMap<>();
    map.put("sam", sam);
    map.put("bob", bob);
    Object retValue = expressionEvaluator.evaluate(expr, map);
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  /**
   * Should evaluate with default prefix.
   */
  @Test
  public void shouldEvaluateWithDefaultPrefix() {
    String expr = "sam.age < bob.age && sam.address.city.length() > ${address.city.length()}";
    Map<String, Object> map = new HashMap<>();
    map.put("sam", sam);
    map.put("bob", bob);
    Object retValue = expressionEvaluator.evaluate(expr, map, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }
}
