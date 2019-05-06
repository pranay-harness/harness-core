package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.User.Builder.anUser;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.exception.WingsExceptionMapper;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;
import software.wings.utils.CacheHelper;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class UserResourceTest {
  public static final UserService USER_SERVICE = mock(UserService.class);
  public static final HarnessUserGroupService HARNESS_USER_GROUP_SERVICE = mock(HarnessUserGroupService.class);
  public static final UserGroupService USER_GROUP_SERVICE = mock(UserGroupService.class);
  public static final CacheHelper CACHE_HELPER = mock(CacheHelper.class);
  public static final AuthService AUTH_SERVICE = mock(AuthService.class);
  public static final AccountService ACCOUNT_SERVICE = mock(AccountService.class);
  public static final AuthenticationManager AUTHENTICATION_MANAGER = mock(AuthenticationManager.class);
  public static final TwoFactorAuthenticationManager TWO_FACTOR_AUTHENTICATION_MANAGER =
      mock(TwoFactorAuthenticationManager.class);
  static final UsageRestrictionsService USAGE_RESTRICTIONS_SERVICE = mock(UsageRestrictionsService.class);
  static final AccountPermissionUtils ACCOUNT_PERMISSION_UTILS = mock(AccountPermissionUtils.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .addResource(new UserResource(USER_SERVICE, AUTH_SERVICE, ACCOUNT_SERVICE, USAGE_RESTRICTIONS_SERVICE,
              ACCOUNT_PERMISSION_UTILS, AUTHENTICATION_MANAGER, TWO_FACTOR_AUTHENTICATION_MANAGER, CACHE_HELPER,
              HARNESS_USER_GROUP_SERVICE, USER_GROUP_SERVICE))
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
  @Category(UnitTests.class)
  public void shouldErrorOnListWhenAccountIdIsNotFound() {
    RestResponse<PageResponse<User>> restResponse =
        RESOURCES.client().target("/users").request().get(new GenericType<RestResponse<PageResponse<User>>>() {});
  }
}
