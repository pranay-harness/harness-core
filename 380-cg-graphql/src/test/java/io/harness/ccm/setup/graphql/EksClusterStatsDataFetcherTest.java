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
import io.harness.ccm.commons.entities.billing.CECluster;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
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
public class EksClusterStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject EksClusterStatsDataFetcher eksClusterStatsDataFetcher;

  private static final String INFRA_ACCOUNT_ID = "infraAccountId";
  private static final String INFRA_MASTER_ACCOUNT_ID = "infraMasterAccountId";

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    createAccount(ACCOUNT1_ID, getLicenseInfo());
    CECluster ceCluster = CECluster.builder()
                              .accountId(ACCOUNT1_ID)
                              .clusterName(CLUSTER1_NAME)
                              .infraAccountId(INFRA_ACCOUNT_ID)
                              .infraMasterAccountId(INFRA_MASTER_ACCOUNT_ID)
                              .region(REGION1)
                              .parentAccountSettingId(SETTING_ID1)
                              .build();
    createCECluster(ceCluster);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    List<QLCESetupFilter> filters = Arrays.asList(getInfraMasterAccountIdFilter());
    QLEKSClusterData data = eksClusterStatsDataFetcher.fetchConnection(filters, null, null);
    assertThat(data.getCount()).isEqualTo(1);
    assertThat(data.getClusters().get(0).getInfraAccountId()).isEqualTo(INFRA_ACCOUNT_ID);
    assertThat(data.getClusters().get(0).getId()).isNotNull();
    assertThat(data.getClusters().get(0).getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(data.getClusters().get(0).getInfraMasterAccountId()).isEqualTo(INFRA_MASTER_ACCOUNT_ID);
    assertThat(data.getClusters().get(0).getRegion()).isEqualTo(REGION1);
    assertThat(data.getClusters().get(0).getParentAccountSettingId()).isEqualTo(SETTING_ID1);
  }

  private QLCESetupFilter getCloudProviderIdFilter() {
    return QLCESetupFilter.builder()
        .cloudProviderId(QLIdFilter.builder()
                             .operator(QLIdOperator.EQUALS)
                             .values(new String[] {CLOUD_PROVIDER1_ID_ACCOUNT1})
                             .build())
        .build();
  }

  private QLCESetupFilter getInfraMasterAccountIdFilter() {
    return QLCESetupFilter.builder()
        .infraMasterAccountId(
            QLIdFilter.builder().operator(QLIdOperator.NOT_IN).values(new String[] {INFRA_ACCOUNT_ID}).build())
        .build();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGenerateFilter() {
    QLCESetupFilter filter = eksClusterStatsDataFetcher.generateFilter(null, null, null);
    assertThat(filter).isNull();
  }
}
