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

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.DeploymentPreference;
import software.wings.beans.Preference;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.PreferenceService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PreferenceResourceTest extends WingsBaseTest {
  private static final PreferenceService preferenceService = mock(PreferenceService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new PreferenceResource(preferenceService)).build();
  public static final String TEST_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  public static final String TEST_USER_ID = "123";
  public static final String TEST_PREFERENCE_ID = "jXyGvjmASyao0dNk2-QklQ";

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
  public void shouldGetPreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(ID_KEY);
    when(preferenceService.get(TEST_ACCOUNT_ID, TEST_USER_ID, TEST_PREFERENCE_ID)).thenReturn(deployPref);

    RestResponse<DeploymentPreference> restResponse =
        RESOURCES.client()
            .target(format("/preference/%s?accountId=%s&userId=%s", TEST_PREFERENCE_ID, TEST_ACCOUNT_ID, TEST_USER_ID))
            .request()
            .get(new GenericType<RestResponse<DeploymentPreference>>() {});

    verify(preferenceService, atLeastOnce()).get(TEST_ACCOUNT_ID, TEST_USER_ID, TEST_PREFERENCE_ID);
    assertThat(restResponse.getResource()).isEqualTo(deployPref);
  }

  /**
   * Test LIST preferences
   */
  @Test
  public void shouldListPreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(TEST_PREFERENCE_ID);
    PageResponse<Preference> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(deployPref));
    when(preferenceService.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<Preference>> restResponse =
        RESOURCES.client()
            .target("/preference?accountId=" + TEST_ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Preference>>>() {});
    PageRequest<Preference> pageRequest = new PageRequest<>();
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  /**
   * Test POST preference
   */
  @Test
  public void shouldCreatePreference() {
    DeploymentPreference deployPref = new DeploymentPreference();
    deployPref.setUuid(ID_KEY);
    deployPref.setAccountId(TEST_ACCOUNT_ID);
    deployPref.setUserId(TEST_USER_ID);

    when(preferenceService.save(any(), any(), any())).thenReturn(deployPref);

    RestResponse<DeploymentPreference> restResponse =
        RESOURCES.client()
            .target(format("/preference?accountId=%s&userId=%s", TEST_ACCOUNT_ID, TEST_USER_ID))
            .request()
            .post(entity(deployPref, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DeploymentPreference>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(DeploymentPreference.class);
  }

  /**
   * Test PUT preference
   */
  @Test
  public void shouldUpdatePreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(TEST_PREFERENCE_ID);
    deployPref.setAccountId(TEST_ACCOUNT_ID);
    deployPref.setUserId(TEST_USER_ID);
    when(preferenceService.update(any(), any(), any(), any())).thenReturn(deployPref);

    RestResponse<DeploymentPreference> restResponse =
        RESOURCES.client()
            .target(format("/preference/%s?accountId=%s&userId=%s", TEST_PREFERENCE_ID, TEST_ACCOUNT_ID, TEST_USER_ID))
            .request()
            .put(entity(deployPref, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DeploymentPreference>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(DeploymentPreference.class);
  }

  /**
   * Test DELETE preference
   */
  @Test
  public void shouldDeletePreference() {
    Preference deployPref = new DeploymentPreference();
    deployPref.setUuid(ID_KEY);

    Response restResponse =
        RESOURCES.client()
            .target(format("/preference/%s?accountId=%s&userId=%s", TEST_PREFERENCE_ID, TEST_ACCOUNT_ID, TEST_USER_ID))
            .request()
            .delete();

    verify(preferenceService, atLeastOnce()).delete(TEST_ACCOUNT_ID, TEST_USER_ID, TEST_PREFERENCE_ID);
  }
}
