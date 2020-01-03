package software.wings.beans.sso;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LdapUserResponseTest extends CategoryTest {
  private LdapUserResponse ldapUserResponse;
  private String capitalLettersEmail = "XYZ@harness.io";

  @Before
  public void setup() {
    ldapUserResponse = LdapUserResponse.builder().email(capitalLettersEmail).build();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void getEmailGetterTest() {
    assertThat(ldapUserResponse.getEmail()).isEqualTo(capitalLettersEmail.toLowerCase());
  }
}
