package software.wings.resources;

import static io.harness.rule.OwnerRule.INDER;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.WebHookRequest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WebHookService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebHookResourceTest extends WingsBaseTest {
  private static final WebHookService WEBHOOK_SERVICE = mock(WebHookService.class);
  private static final FeatureFlagService FEATURE_FLAG_SERVICE = mock(FeatureFlagService.class);
  private static final AppService APP_SERVICE = mock(AppService.class);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder()
          .instance(new WebHookResource(WEBHOOK_SERVICE, FEATURE_FLAG_SERVICE, APP_SERVICE))
          .build();

  private final String APP_ID = "APP_ID";
  private final String WEBHOOK_ID = "WEBHOOK_ID";
  private final String API_KEY = "API_KEY";

  @Before
  public void setup() {
    reset(WEBHOOK_SERVICE);
    reset(FEATURE_FLAG_SERVICE);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testWebhookExecuteWithoutFF() {
    WebHookRequest webHookRequest = WebHookRequest.builder().application(APP_ID).build();
    when(WEBHOOK_SERVICE.execute(WEBHOOK_ID, webHookRequest)).thenReturn(Response.noContent().build());
    Response response = resources.client()
                            .target("/webhooks/" + WEBHOOK_ID)
                            .request()
                            .post(Entity.entity(webHookRequest, MediaType.APPLICATION_JSON_TYPE));

    assertThat(response).isNotNull();
    verify(WEBHOOK_SERVICE).execute(WEBHOOK_ID, webHookRequest);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testWebhookExecuteWithFF() {
    when(FEATURE_FLAG_SERVICE.isEnabled(any(), anyString())).thenReturn(true);
    when(APP_SERVICE.get(APP_ID)).thenReturn(anApplication().isManualTriggerAuthorized(true).build());
    WebHookRequest webHookRequest = WebHookRequest.builder().application(APP_ID).build();
    when(WEBHOOK_SERVICE.execute(WEBHOOK_ID, webHookRequest)).thenReturn(Response.noContent().build());

    Response response = resources.client()
                            .target("/webhooks/" + WEBHOOK_ID)
                            .request()
                            .header(API_KEY_HEADER, API_KEY)
                            .post(Entity.entity(webHookRequest, MediaType.APPLICATION_JSON_TYPE));

    assertThat(response).isNotNull();
    verify(WEBHOOK_SERVICE).execute(WEBHOOK_ID, webHookRequest);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testWebhookExecuteWithFFWithoutApiKey() {
    when(FEATURE_FLAG_SERVICE.isEnabled(any(), anyString())).thenReturn(true);
    when(APP_SERVICE.get(APP_ID)).thenReturn(anApplication().isManualTriggerAuthorized(true).build());
    WebHookRequest webHookRequest = WebHookRequest.builder().application(APP_ID).build();
    when(WEBHOOK_SERVICE.execute(WEBHOOK_ID, webHookRequest)).thenReturn(Response.noContent().build());

    assertThatThrownBy(()
                           -> resources.client()
                                  .target("/webhooks/" + WEBHOOK_ID)
                                  .request()
                                  .post(Entity.entity(webHookRequest, MediaType.APPLICATION_JSON_TYPE)))
        .getCause()
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Api Key cannot be empty");
    verify(WEBHOOK_SERVICE, never()).execute(WEBHOOK_ID, webHookRequest);
  }
}
