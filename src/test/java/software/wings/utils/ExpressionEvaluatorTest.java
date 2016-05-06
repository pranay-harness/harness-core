/**
 *
 */
package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseUnitTest;
import software.wings.beans.Host;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * @author Rishi
 *
 */
public class ExpressionEvaluatorTest extends WingsBaseUnitTest {
  @Inject private ExpressionEvaluator expressionEvaluator;

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

  public static class Person {
    private Address address;
    private int age;

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  public static class Address {
    private String city;

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }
  }
}
