package software.wings.beans.sso;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LdapUserResponseTest {
  private LdapUserResponse ldapUserResponse;
  private String capitalLettersEmail = "XYZ@harness.io";

  @Before
  public void setup() {
    ldapUserResponse = LdapUserResponse.builder().email(capitalLettersEmail).build();
  }

  @Test
  @Category(UnitTests.class)
  public void getEmailGetterTest() {
    assertEquals(capitalLettersEmail.toLowerCase(), ldapUserResponse.getEmail());
  }
}
