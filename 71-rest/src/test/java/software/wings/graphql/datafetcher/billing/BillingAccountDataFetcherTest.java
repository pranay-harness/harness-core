package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
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

public class BillingAccountDataFetcherTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String uuid = "UUID";
  private String organizationSettingId = "ORGANIZATION_SETTING_ID";
  private GcpBillingAccount gcpBillingAccount;
  private List<GcpBillingAccount> gcpBillingAccounts;

  @Mock private DataFetchingEnvironment environment;
  @Mock private GraphQLContext graphQLContext;

  @Mock GcpBillingAccountService gcpBillingAccountService;
  @InjectMocks BillingAccountDataFetcher billingAccountDataFetcher;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    gcpBillingAccount = GcpBillingAccount.builder().build();
    gcpBillingAccounts = Arrays.asList(gcpBillingAccount);
    when(environment.getContext()).thenReturn(graphQLContext);
    when(graphQLContext.get(eq("accountId"))).thenReturn(accountId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetGcpBillingAccount() throws Exception {
    when(environment.getArgument(eq("uuid"))).thenReturn(uuid);
    when(gcpBillingAccountService.get(eq(uuid))).thenReturn(gcpBillingAccount);
    List<GcpBillingAccount> actuals = billingAccountDataFetcher.get(environment);
    assertThat(actuals).contains(gcpBillingAccount);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListGcpBillingAccount() throws Exception {
    when(environment.getArgument(eq("organizationSettingId"))).thenReturn(organizationSettingId);
    when(gcpBillingAccountService.list(eq(accountId), eq(organizationSettingId))).thenReturn(gcpBillingAccounts);
    List<GcpBillingAccount> actuals = billingAccountDataFetcher.get(environment);
    assertThat(actuals).containsAll(gcpBillingAccounts);
  }
}
