package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.User.Builder.anUser;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.LoginRequest;
import software.wings.beans.User;
import software.wings.exception.WingsExceptionMapper;
import software.wings.scheduler.AccountPasswordExpirationJob;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.service.impl.ReCaptchaVerifier;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;
import software.wings.utils.CacheManager;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import java.util.Base64;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class UserResourceTest extends WingsBaseTest {
  public static final UserService USER_SERVICE = mock(UserService.class);
  public static final HarnessUserGroupService HARNESS_USER_GROUP_SERVICE = mock(HarnessUserGroupService.class);
  public static final UserGroupService USER_GROUP_SERVICE = mock(UserGroupService.class);
  public static final CacheManager CACHE_HELPER = mock(CacheManager.class);
  public static final AuthService AUTH_SERVICE = mock(AuthService.class);
  public static final AccountService ACCOUNT_SERVICE = mock(AccountService.class);
  public static final MainConfiguration MAIN_CONFIGURATION = mock(MainConfiguration.class);
  public static final AuthenticationManager AUTHENTICATION_MANAGER = mock(AuthenticationManager.class);
  public static final AccountPasswordExpirationJob ACCOUNT_PASSWORD_EXPIRATION_JOB =
      mock(AccountPasswordExpirationJob.class);
  public static final ReCaptchaVerifier RE_CAPTCHA_VERIFIER = mock(ReCaptchaVerifier.class);
  public static final TwoFactorAuthenticationManager TWO_FACTOR_AUTHENTICATION_MANAGER =
      mock(TwoFactorAuthenticationManager.class);
  static final AccountPermissionUtils ACCOUNT_PERMISSION_UTILS = mock(AccountPermissionUtils.class);
  @Inject @InjectMocks private UserResource userResource;

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .addResource(new UserResource(USER_SERVICE, AUTH_SERVICE, ACCOUNT_SERVICE, ACCOUNT_PERMISSION_UTILS,
              AUTHENTICATION_MANAGER, TWO_FACTOR_AUTHENTICATION_MANAGER, CACHE_HELPER, HARNESS_USER_GROUP_SERVICE,
              USER_GROUP_SERVICE, MAIN_CONFIGURATION, ACCOUNT_PASSWORD_EXPIRATION_JOB, RE_CAPTCHA_VERIFIER))
          .addProvider(WingsExceptionMapper.class)
          .addProvider(MultiPartFeature.class)
          .build();

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    reset(USER_SERVICE);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldListUsers() {
    when(USER_SERVICE.list(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse().withResponse(asList(anUser().build())).build());
    RestResponse<PageResponse<User>> restResponse = RESOURCES.client()
                                                        .target("/users?accountId=ACCOUNT_ID")
                                                        .request()
                                                        .get(new GenericType<RestResponse<PageResponse<User>>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(PageResponse.class);
    verify(USER_SERVICE).list(any(PageRequest.class), anyBoolean());
  }

  @Test(expected = BadRequestException.class)
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldErrorOnListWhenAccountIdIsNotFound() {
    RestResponse<PageResponse<User>> restResponse =
        RESOURCES.client().target("/users").request().get(new GenericType<RestResponse<PageResponse<User>>>() {});
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void shouldLoginUserUsingPostRequest() {
    when(AUTHENTICATION_MANAGER.defaultLoginAccount(anyString(), anyString())).thenReturn(new User());
    String username = "userEmail";
    String password = "userPassword";
    String actualString = username + ":" + password;
    String BasicBase64format = Base64.getEncoder().encodeToString(actualString.getBytes());
    String authorization = "Basic " + BasicBase64format;
    userResource.login(LoginRequest.builder().authorization(authorization).build(), null, null);
    verify(AUTHENTICATION_MANAGER, times(1)).defaultLoginAccount(anyString(), anyString());
  }
}
