package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import graphql.GraphQLContext;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.GcpBillingAccountQueryArguments;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

public class GcpBillingAccountDataFetcherTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String uuid = "UUID";
  private String organizationSettingId = "ORGANIZATION_SETTING_ID";
  private GcpBillingAccount gcpBillingAccount;
  private List<GcpBillingAccount> gcpBillingAccounts;

  @Mock private GraphQLContext graphQLContext;

  @Mock GcpBillingAccountService gcpBillingAccountService;
  @InjectMocks GcpBillingAccountDataFetcher gcpBillingAccountDataFetcher;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    gcpBillingAccount = GcpBillingAccount.builder().build();
    gcpBillingAccounts = Arrays.asList(gcpBillingAccount);
    when(graphQLContext.get(eq("accountId"))).thenReturn(accountId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetGcpBillingAccount() throws Exception {
    when(gcpBillingAccountService.get(eq(uuid))).thenReturn(gcpBillingAccount);
    GcpBillingAccountQueryArguments arguments = new GcpBillingAccountQueryArguments(uuid, organizationSettingId);
    List<GcpBillingAccount> actuals = gcpBillingAccountDataFetcher.fetch(arguments, accountId);
    assertThat(actuals).contains(gcpBillingAccount);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListGcpBillingAccount() throws Exception {
    when(gcpBillingAccountService.list(eq(accountId), eq(organizationSettingId))).thenReturn(gcpBillingAccounts);
    GcpBillingAccountQueryArguments arguments = new GcpBillingAccountQueryArguments(null, organizationSettingId);
    List<GcpBillingAccount> actuals = gcpBillingAccountDataFetcher.fetch(arguments, accountId);
    assertThat(actuals).containsAll(gcpBillingAccounts);
  }
}
