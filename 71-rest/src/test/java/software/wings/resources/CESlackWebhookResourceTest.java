package software.wings.resources;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class CESlackWebhookResourceTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private CESlackWebhook ceSlackWebhook;

  private static CESlackWebhookService ceSlackWebhookService = mock(CESlackWebhookService.class);

  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new CESlackWebhookResource(ceSlackWebhookService)).build();
  @Before
  public void setUp() {
    ceSlackWebhook = CESlackWebhook.builder().accountId(accountId).webhookUrl("WEBHOOK_URL").build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    RESOURCES.client()
        .target(format("/ceSlackWebhooks/?accountId=%s", accountId))
        .request()
        .get(new GenericType<RestResponse<CESlackWebhook>>() {});
    verify(ceSlackWebhookService).getByAccountId(eq(accountId));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSave() {
    RESOURCES.client()
        .target(format("/ceSlackWebhooks/?accountId=%s", accountId))
        .request()
        .post(entity(ceSlackWebhook, MediaType.APPLICATION_JSON), new GenericType<RestResponse<CESlackWebhook>>() {});
    verify(ceSlackWebhookService).upsert(eq(ceSlackWebhook));
  }
}
