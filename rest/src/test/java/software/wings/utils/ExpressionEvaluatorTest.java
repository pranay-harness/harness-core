/**
 *
 */

package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

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
    Person sam = new Person();
    sam.setAge(20);
    Address address = new Address();
    address.setCity("San Francisco");
    sam.setAddress(address);

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
    Person sam = new Person();
    sam.setAge(20);
    Address address = new Address();
    address.setCity("San Francisco");
    sam.setAddress(address);

    Person bob = new Person();
    bob.setAge(40);
    address = new Address();
    address.setCity("New York");
    bob.setAddress(address);

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
    Person sam = new Person();
    sam.setAge(20);
    Address address = new Address();
    address.setCity("San Francisco");
    sam.setAddress(address);

    Person bob = new Person();
    bob.setAge(40);
    address = new Address();
    address.setCity("New York");
    bob.setAddress(address);

    String expr = "sam.age < bob.age && sam.address.city.length() > ${address.city.length()}";
    Map<String, Object> map = new HashMap<>();
    map.put("sam", sam);
    map.put("bob", bob);
    Object retValue = expressionEvaluator.evaluate(expr, map, "bob");
    assertThat(retValue).isNotNull();
    assertThat(retValue).isInstanceOf(Boolean.class);
    assertThat(retValue).isEqualTo(true);
  }

  /**
   * The Class Person.
   */
  public static class Person {
    private Address address;
    private int age;

    /**
     * Gets address.
     *
     * @return the address
     */
    public Address getAddress() {
      return address;
    }

    /**
     * Sets address.
     *
     * @param address the address
     */
    public void setAddress(Address address) {
      this.address = address;
    }

    /**
     * Gets age.
     *
     * @return the age
     */
    public int getAge() {
      return age;
    }

    /**
     * Sets age.
     *
     * @param age the age
     */
    public void setAge(int age) {
      this.age = age;
    }
  }

  /**
   * The Class Address.
   */
  public static class Address {
    private String city;

    /**
     * Gets city.
     *
     * @return the city
     */
    public String getCity() {
      return city;
    }

    /**
     * Sets city.
     *
     * @param city the city
     */
    public void setCity(String city) {
      this.city = city;
    }
  }
}
