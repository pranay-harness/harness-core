package software.wings.resources;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class UserResourceTest {
  public static final UserService USER_SERVICE = mock(UserService.class);
  public static final AccountService ACCOUNT_SERVICE = mock(AccountService.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new UserResource(USER_SERVICE, ACCOUNT_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
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
  public void shouldListUsers() {
    when(USER_SERVICE.list(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(anUser().build())).build());
    RestResponse<PageResponse<User>> restResponse = RESOURCES.client()
                                                        .target("/users?accountId=ACCOUNT_ID")
                                                        .request()
                                                        .get(new GenericType<RestResponse<PageResponse<User>>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(PageResponse.class);
    verify(USER_SERVICE).list(any(PageRequest.class));
  }

  @Test(expected = BadRequestException.class)
  public void shouldErrorOnListWhenAccountIdIsNotFound() {
    RestResponse<PageResponse<User>> restResponse =
        RESOURCES.client().target("/users").request().get(new GenericType<RestResponse<PageResponse<User>>>() {});
  }
}
