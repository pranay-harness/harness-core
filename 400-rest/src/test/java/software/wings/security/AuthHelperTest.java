package software.wings.security;

import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.resources.AccountResource;
import software.wings.resources.secretsmanagement.SecretsResourceNG;

import com.google.inject.Inject;
import java.lang.reflect.Method;
import javax.ws.rs.container.ResourceInfo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AuthHelperTest extends WingsBaseTest {
  @Mock ResourceInfo resourceInfo = mock(ResourceInfo.class);
  @InjectMocks private AuthHelper authHelper;

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testIsNextGenManagerRequest_For_NextGenAuthorization() {
    Class clazz = SecretsResourceNG.class;
    when(resourceInfo.getResourceClass()).thenReturn(clazz);
    when(resourceInfo.getResourceMethod()).thenReturn(getMockResourceMethod());

    boolean isAuthorizationValid = authHelper.isNextGenManagerAPI();
    assertThat(isAuthorizationValid).isTrue();

    clazz = AccountResource.class;
    when(resourceInfo.getResourceClass()).thenReturn(clazz);

    isAuthorizationValid = authHelper.isNextGenManagerAPI();
    assertThat(isAuthorizationValid).isFalse();
  }

  private Method getMockResourceMethod() {
    Class mockClass = SecretsResourceNG.class;
    try {
      return mockClass.getMethod("get", String.class, String.class, String.class, String.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
