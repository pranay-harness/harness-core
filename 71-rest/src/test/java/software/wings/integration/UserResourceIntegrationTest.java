package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.FeatureName.LOGIN_PROMPT_WHEN_NO_USER;

import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Integration test class for UserResource class.
 */
public class UserResourceIntegrationTest extends BaseIntegrationTest {
  final String getLoginTypeURI = "/users/logintype";

  @Before
  public void setUp() {
    enableLoginPromptWhenNoUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLoginTypeResponseForNewAdminUserShouldReturnUserPassWord() {
    String argument = "userName=admin@harness.io";
    String url = getLoginTypeResponseUri(getLoginTypeURI, argument);
    WebTarget target = client.target(url);
    LoginTypeResponse response =
        target.request().get(new GenericType<RestResponse<LoginTypeResponse>>() {}).getResource();
    assertThat(response.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLoginTypeResponseForNonExistentUserShouldReturnUserPassWord() {
    String nonExistingUserArgument = "userName=random@xyz";
    String url = getLoginTypeResponseUri(getLoginTypeURI, nonExistingUserArgument);
    WebTarget target = client.target(url);
    LoginTypeResponse response =
        target.request().get(new GenericType<RestResponse<LoginTypeResponse>>() {}).getResource();
    assertThat(response.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
  }

  private String getLoginTypeResponseUri(final String uri, final String arguments) {
    return API_BASE + uri + "?" + arguments;
  }

  private void enableLoginPromptWhenNoUser() {
    FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class)
                                  .filter(FeatureFlagKeys.name, LOGIN_PROMPT_WHEN_NO_USER.name())
                                  .get();

    if (featureFlag == null) {
      featureFlag = FeatureFlag.builder().name(LOGIN_PROMPT_WHEN_NO_USER.name()).enabled(true).build();
    } else {
      featureFlag.setEnabled(true);
    }
    wingsPersistence.save(featureFlag);
  }
}
