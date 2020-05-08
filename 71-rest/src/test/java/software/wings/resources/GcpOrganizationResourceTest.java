package software.wings.resources;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ValidationResult;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class GcpOrganizationResourceTest {
  private String accountId = "ACCOUNT_ID";
  private GcpOrganization gcpOrganization;

  private static GcpOrganizationService gcpOrganizationService = mock(GcpOrganizationService.class);
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new GcpOrganizationResource(gcpOrganizationService)).build();

  @Before
  public void setUp() {
    gcpOrganization = GcpOrganization.builder().accountId(accountId).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldValidatePermission() {
    RESOURCES.client()
        .target(format("/gcp-organizations/validate-serviceaccount/?accountId=%s", accountId))
        .request()
        .post(
            entity(gcpOrganization, MediaType.APPLICATION_JSON), new GenericType<RestResponse<ValidationResult>>() {});
    verify(gcpOrganizationService).validate(gcpOrganization);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldSave() {
    RESOURCES.client()
        .target(format("/gcp-organizations/?accountId=%s", accountId))
        .request()
        .post(entity(gcpOrganization, MediaType.APPLICATION_JSON), new GenericType<RestResponse<GcpOrganization>>() {});
    verify(gcpOrganizationService).upsert(gcpOrganization);
  }
}
