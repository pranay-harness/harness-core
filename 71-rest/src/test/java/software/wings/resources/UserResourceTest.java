package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.MEHUL;
import static io.harness.rule.OwnerRule.MOHIT;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.ErrorData;
import software.wings.beans.LoginRequest;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
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
import software.wings.signup.BugsnagErrorReporter;
import software.wings.utils.AccountPermissionUtils;
import software.wings.utils.CacheManager;
import software.wings.utils.ResourceTestRule;
import wiremock.com.google.common.collect.Lists;

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
  @Mock private BugsnagErrorReporter bugsnagErrorReporter;

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
    PageRequest pageRequest = mock(PageRequest.class);
    when(pageRequest.getOffset()).thenReturn("0");
    when(pageRequest.getPageSize()).thenReturn(30);
    when(USER_SERVICE.listUsers(any(), anyBoolean(), anyInt(), anyInt(), anyBoolean()))
        .thenReturn(aPageResponse().withResponse(asList(anUser().build())).build());
    userResource.list(pageRequest, UUIDGenerator.generateUuid(), null, false);
    verify(USER_SERVICE).getTotalUserCount(any(), anyBoolean());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void shouldSearchUsers() {
    PageRequest pageRequest = mock(PageRequest.class);
    when(pageRequest.getOffset()).thenReturn("0");
    when(pageRequest.getPageSize()).thenReturn(30);
    when(USER_SERVICE.searchUsers(anyString(), anyBoolean(), anyInt(), anyInt(), anyString()))
        .thenReturn(Lists.newArrayList());
    userResource.list(pageRequest, UUIDGenerator.generateUuid(), "xyz", false);
    verify(USER_SERVICE).searchUsers(any(), anyBoolean(), anyInt(), any(), anyString());
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

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldCheckInvite() {
    String accountId = UUIDGenerator.generateUuid();
    String inviteId = UUIDGenerator.generateUuid();
    userResource.checkInvite(accountId, inviteId);
    verify(USER_SERVICE, times(1)).checkInviteStatus(any());
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldCompleteInvite() {
    String accountId = UUIDGenerator.generateUuid();
    String inviteId = UUIDGenerator.generateUuid();
    UserInvite userInvite = anUserInvite().build();
    userResource.completeInvite(accountId, inviteId, userInvite);
    verify(USER_SERVICE, times(1)).completeInvite(any());
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldInviteUsers() {
    String accountId = UUIDGenerator.generateUuid();
    String inviteId = UUIDGenerator.generateUuid();
    UserInvite userInvite = anUserInvite().build();
    userResource.inviteUsers(accountId, userInvite);
    verify(USER_SERVICE, times(1)).inviteUsers(any());
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void testBugsnagErrorReportingForTrialSignup() {
    UserInvite userInvite = anUserInvite().withEmail("invalid").build();
    try {
      userResource.trialSignup(userInvite);
    } catch (Exception e) {
      verify(bugsnagErrorReporter, times(1)).report(ErrorData.builder().exception(e).email("invalid").build());
      assertThat(e).isNotNull();
    }
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void testBugsnagErrorReportingForResendVerificationEmail() {
    try {
      userResource.resendVerificationEmail("");
    } catch (Exception e) {
      verify(bugsnagErrorReporter, times(1)).report(ErrorData.builder().exception(e).email("").build());
      assertThat(e).isNotNull();
    }
  }
}
