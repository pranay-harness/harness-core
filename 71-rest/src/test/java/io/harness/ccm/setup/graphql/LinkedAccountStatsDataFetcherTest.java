package io.harness.ccm.setup.graphql;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.ce.CECloudAccount;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class LinkedAccountStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Inject LinkedAccountStatsDataFetcher linkedAccountStatsDataFetcher;

  private static final String INFRA_ACCOUNT_ID = "infraAccountId";
  private static final String INFRA_MASTER_ACCOUNT_ID = "infraMasterAccountId";
  private static final String INFRA_ACCOUNT_ARN = "infraAccountArn";
  private static final String INFRA_ACCOUNT_NAME = "infraAccountName";

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    createAccount(ACCOUNT1_ID, getLicenseInfo());
    CECloudAccount ceCloudAccount = CECloudAccount.builder()
                                        .accountId(ACCOUNT1_ID)
                                        .infraAccountId(INFRA_ACCOUNT_ID)
                                        .infraMasterAccountId(INFRA_MASTER_ACCOUNT_ID)
                                        .accountArn(INFRA_ACCOUNT_ARN)
                                        .accountName(INFRA_ACCOUNT_NAME)
                                        .masterAccountSettingId(SETTING_ID1)
                                        .build();
    createCECloudAccount(ceCloudAccount);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    List<QLCESetupFilter> filters = Arrays.asList(getMasterAccountSettingIdFilter());
    QLLinkedAccountData data = linkedAccountStatsDataFetcher.fetchConnection(filters, null, null);
    assertThat(data.getCount()).isEqualTo(1);
    assertThat(data.getLinkedAccounts().get(0).getId()).isEqualTo(INFRA_ACCOUNT_ID);
    assertThat(data.getLinkedAccounts().get(0).getName()).isEqualTo(INFRA_ACCOUNT_NAME);
    assertThat(data.getLinkedAccounts().get(0).getArn()).isEqualTo(INFRA_ACCOUNT_ARN);
    assertThat(data.getLinkedAccounts().get(0).getMasterAccountId()).isEqualTo(INFRA_MASTER_ACCOUNT_ID);
  }

  private QLCESetupFilter getMasterAccountSettingIdFilter() {
    return QLCESetupFilter.builder()
        .masterAccountSettingId(
            QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {SETTING_ID1}).build())
        .build();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testPopulateFilters() {
    QLCESetupFilter filter = linkedAccountStatsDataFetcher.generateFilter(null, null, null);
    assertThat(filter).isNull();
  }
}