package software.wings.resources;

import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.VIKAS;
import static java.lang.String.format;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.datahandler.services.AdminAccountService;
import io.harness.datahandler.services.AdminUserService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

public class AdminAccountResourceTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private Account account;

  private static AdminAccountService adminAccountService = mock(AdminAccountService.class);
  private static AdminUserService adminUserService = mock(AdminUserService.class);

  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new AdminAccountResource(adminAccountService, adminUserService)).build();

  @Before
  public void setUp() {
    account = anAccount()
                  .withUuid(accountId)
                  .withAccountKey("ACCOUNT_KEY")
                  .withCloudCostEnabled(true)
                  .withCeK8sEventCollectionEnabled(true)
                  .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldEnableOrDisableCeAutoCollectK8sEvents() {
    RESOURCES.client()
        .target(format("/admin/accounts/%s/ceAutoCollectK8sEvents/?enable=%b", accountId, true))
        .request()
        .put(Entity.json(""), new GenericType<RestResponse<Boolean>>() {});
    verify(adminAccountService).enableOrDisableCeK8sEventCollection(eq(accountId), eq(true));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testUpdatePovFlag() {
    RESOURCES.client()
        .target(format("/admin/accounts/%s/pov?isPov=%b", accountId, true))
        .request()
        .put(Entity.json(""), new GenericType<RestResponse<Boolean>>() {});
    verify(adminAccountService).updatePovFlag(eq(accountId), eq(true));
  }
}
