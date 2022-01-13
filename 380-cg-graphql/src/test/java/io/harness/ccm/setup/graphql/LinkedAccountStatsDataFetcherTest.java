/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.entities.billing.CECloudAccount.AccountStatus;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class LinkedAccountStatsDataFetcherTest extends AbstractDataFetcherTestBase {
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
    CECloudAccount ceCloudAccountConnected = CECloudAccount.builder()
                                                 .accountId(ACCOUNT1_ID)
                                                 .infraAccountId(INFRA_ACCOUNT_ID)
                                                 .infraMasterAccountId(INFRA_MASTER_ACCOUNT_ID)
                                                 .accountArn(INFRA_ACCOUNT_ARN)
                                                 .accountName(INFRA_ACCOUNT_NAME)
                                                 .masterAccountSettingId(SETTING_ID1)
                                                 .accountStatus(AccountStatus.CONNECTED)
                                                 .build();

    CECloudAccount ceCloudAccountNotConnected = CECloudAccount.builder()
                                                    .accountId(ACCOUNT1_ID)
                                                    .infraAccountId(INFRA_ACCOUNT_ID)
                                                    .infraMasterAccountId(INFRA_MASTER_ACCOUNT_ID)
                                                    .accountArn(INFRA_ACCOUNT_ARN)
                                                    .accountName(INFRA_ACCOUNT_NAME)
                                                    .masterAccountSettingId(SETTING_ID1)
                                                    .accountStatus(AccountStatus.NOT_CONNECTED)
                                                    .build();

    CECloudAccount ceCloudAccountNotVerified = CECloudAccount.builder()
                                                   .accountId(ACCOUNT1_ID)
                                                   .infraAccountId(INFRA_ACCOUNT_ID)
                                                   .infraMasterAccountId(INFRA_MASTER_ACCOUNT_ID)
                                                   .accountArn(INFRA_ACCOUNT_ARN)
                                                   .accountName(INFRA_ACCOUNT_NAME)
                                                   .masterAccountSettingId(SETTING_ID1)
                                                   .accountStatus(AccountStatus.NOT_VERIFIED)
                                                   .build();

    createCECloudAccount(ceCloudAccountConnected);
    createCECloudAccount(ceCloudAccountNotConnected);
    createCECloudAccount(ceCloudAccountNotVerified);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchConnectionWithASCSort() {
    List<QLCESetupFilter> filters = Arrays.asList(getMasterAccountSettingIdFilter());
    List<QLCESetupSortCriteria> sortCriteria = Arrays.asList(getSortByASCAccountStatus());
    QLLinkedAccountData data = linkedAccountStatsDataFetcher.fetchConnection(filters, null, sortCriteria);
    assertThat(data.getLinkedAccounts().get(0).getId()).isEqualTo(INFRA_ACCOUNT_ID);
    assertThat(data.getLinkedAccounts().get(0).getName()).isEqualTo(INFRA_ACCOUNT_NAME);
    assertThat(data.getLinkedAccounts().get(0).getArn()).isEqualTo(INFRA_ACCOUNT_ARN);
    assertThat(data.getLinkedAccounts().get(0).getMasterAccountId()).isEqualTo(INFRA_MASTER_ACCOUNT_ID);
    assertThat(data.getLinkedAccounts().get(0).getAccountStatus()).isEqualTo(AccountStatus.CONNECTED);
    assertThat(data.getLinkedAccounts().get(1).getAccountStatus()).isEqualTo(AccountStatus.NOT_CONNECTED);
    assertThat(data.getLinkedAccounts().get(2).getAccountStatus()).isEqualTo(AccountStatus.NOT_VERIFIED);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    List<QLCESetupFilter> filters = Arrays.asList(getMasterAccountSettingIdFilter());
    List<QLCESetupSortCriteria> sortCriteria = Arrays.asList(getSortByDESCAccountStatus());
    QLLinkedAccountData data = linkedAccountStatsDataFetcher.fetchConnection(filters, null, sortCriteria);
    assertThat(data.getCount().getCountOfConnected()).isEqualTo(1);
    assertThat(data.getCount().getCountOfNotConnected()).isEqualTo(1);
    assertThat(data.getCount().getCountOfNotVerified()).isEqualTo(1);
    assertThat(data.getLinkedAccounts().get(0).getAccountStatus()).isEqualTo(AccountStatus.NOT_VERIFIED);
    assertThat(data.getLinkedAccounts().get(1).getAccountStatus()).isEqualTo(AccountStatus.NOT_CONNECTED);
    assertThat(data.getLinkedAccounts().get(2).getAccountStatus()).isEqualTo(AccountStatus.CONNECTED);
  }

  private QLCESetupFilter getMasterAccountSettingIdFilter() {
    return QLCESetupFilter.builder()
        .masterAccountSettingId(
            QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {SETTING_ID1}).build())
        .build();
  }

  private QLCESetupSortCriteria getSortByDESCAccountStatus() {
    return QLCESetupSortCriteria.builder().sortOrder(QLSortOrder.DESCENDING).sortType(QLCESetupSortType.status).build();
  }

  private QLCESetupSortCriteria getSortByASCAccountStatus() {
    return QLCESetupSortCriteria.builder().sortOrder(QLSortOrder.ASCENDING).sortType(QLCESetupSortType.status).build();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testPopulateFilters() {
    QLCESetupFilter filter = linkedAccountStatsDataFetcher.generateFilter(null, null, null);
    assertThat(filter).isNull();
  }
}
