package software.wings.resources;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.security.UserThreadLocal.userGuard;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.DeploymentPreference;
import software.wings.beans.Preference;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.PreferenceService;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PreferenceResourceTest extends WingsBaseTest {
  private static final PreferenceService preferenceService = mock(PreferenceService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new PreferenceResource(preferenceService)).build();
  public static final String PREFERENCE_ID = "PREFERENCE_ID";

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(preferenceService);
  }

  /**
   * Test GET preference
   */
  @Test
  @Category(UnitTests.class)
  public void shouldGetPreference() throws IOException {
    try (UserThreadLocal.Guard guard =
             userGuard(User.Builder.anUser().withUuid("USER_ID").withName("USER_ID").build())) {
      Preference deployPref = new DeploymentPreference();
      when(preferenceService.get(ACCOUNT_ID, USER_ID, PREFERENCE_ID)).thenReturn(deployPref);

      RestResponse<DeploymentPreference> restResponse =
          RESOURCES.client()
              .target(format("/preference/%s?accountId=%s", PREFERENCE_ID, ACCOUNT_ID))
              .request()
              .get(new GenericType<RestResponse<DeploymentPreference>>() {});

      verify(preferenceService, atLeastOnce()).get(ACCOUNT_ID, USER_ID, PREFERENCE_ID);
      assertThat(restResponse.getResource()).isEqualTo(deployPref);
    }
  }

  /**
   * Test LIST preferences
   */
  @Test
  @Category(UnitTests.class)
  public void shouldListPreference() throws IOException {
    try (UserThreadLocal.Guard guard =
             userGuard(User.Builder.anUser().withUuid("USER_ID").withName("USER_ID").build())) {
      Preference deployPref = new DeploymentPreference();
      deployPref.setUuid(PREFERENCE_ID);
      PageResponse<Preference> pageResponse = new PageResponse<>();
      pageResponse.setResponse(asList(deployPref));
      when(preferenceService.list(any(PageRequest.class), any())).thenReturn(pageResponse);
      RestResponse<PageResponse<Preference>> restResponse =
          RESOURCES.client()
              .target("/preference?accountId=" + ACCOUNT_ID)
              .request()
              .get(new GenericType<RestResponse<PageResponse<Preference>>>() {});
      assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
    }
  }

  /**
   * Test POST preference
   */
  @Test
  @Category(UnitTests.class)
  public void shouldCreatePreference() throws IOException {
    try (UserThreadLocal.Guard guard =
             userGuard(User.Builder.anUser().withUuid("USER_ID").withName("USER_ID").build())) {
      DeploymentPreference deployPref = new DeploymentPreference();
      deployPref.setUuid(ID_KEY);
      deployPref.setAccountId(ACCOUNT_ID);
      deployPref.setUserId(USER_ID);

      when(preferenceService.save(any(), any(), any())).thenReturn(deployPref);

      RestResponse<DeploymentPreference> restResponse =
          RESOURCES.client()
              .target(format("/preference?accountId=%s&userId=%s", ACCOUNT_ID, USER_ID))
              .request()
              .post(entity(deployPref, MediaType.APPLICATION_JSON),
                  new GenericType<RestResponse<DeploymentPreference>>() {});

      assertThat(restResponse.getResource()).isInstanceOf(DeploymentPreference.class);
    }
  }

  /**
   * Test PUT preference
   */
  @Test
  @Category(UnitTests.class)
  public void shouldUpdatePreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(PREFERENCE_ID);
    deployPref.setAccountId(ACCOUNT_ID);
    deployPref.setUserId(USER_ID);
    when(preferenceService.update(any(), any(), any(), any())).thenReturn(deployPref);
    User user = User.Builder.anUser().withUuid(UUID.randomUUID().toString()).withName("USER_ID").build();
    UserThreadLocal.set(user);

    RestResponse<DeploymentPreference> restResponse =
        RESOURCES.client()
            .target(format("/preference/%s?accountId=%s", PREFERENCE_ID, ACCOUNT_ID, USER_ID))
            .request()
            .put(entity(deployPref, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DeploymentPreference>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(DeploymentPreference.class);
  }

  /**
   * Test DELETE preference
   */
  @Test
  @Category(UnitTests.class)
  public void shouldDeletePreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(ID_KEY);

    User user = User.Builder.anUser().withUuid(UUID.randomUUID().toString()).withName("USER_ID").build();
    UserThreadLocal.set(user);

    Response restResponse =
        RESOURCES.client().target(format("/preference/%s?accountId=%s&", PREFERENCE_ID, ACCOUNT_ID)).request().delete();
    assertThat(restResponse.getStatus()).isEqualTo(200);
  }
}
