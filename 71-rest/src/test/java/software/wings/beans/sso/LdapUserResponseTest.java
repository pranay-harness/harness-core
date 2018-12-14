package software.wings.beans.sso;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class LdapUserResponseTest {
  private LdapUserResponse ldapUserResponse;
  private String capitalLettersEmail = "XYZ@harness.io";

  @Before
  public void setup() {
    ldapUserResponse = LdapUserResponse.builder().email(capitalLettersEmail).build();
  }

  @Test
  public void getEmailGetterTest() {
    assertEquals(capitalLettersEmail.toLowerCase(), ldapUserResponse.getEmail());
  }
}
