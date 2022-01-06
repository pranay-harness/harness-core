/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLCloudProvidersQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CloudProviderConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject CloudProviderConnectionDataFetcher cloudProviderDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    createCloudProvider(ACCOUNT1_ID, APP1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCloudProviderDataFetcher() {
    List<QLCloudProviderFilter> filters =
        Arrays.asList(QLCloudProviderFilter.builder()
                          .cloudProvider(QLIdFilter.builder()
                                             .operator(QLIdOperator.EQUALS)
                                             .values(new String[] {CLOUD_PROVIDER1_ID_ACCOUNT1})
                                             .build())
                          .build());

    QLCloudProviderConnection qlCloudProviderConnection = cloudProviderDataFetcher.fetchConnection(filters,
        QLCloudProvidersQueryParameters.builder().limit(2).offset(0).accountId(ACCOUNT1_ID).selectionSet(null).build(),
        null);

    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProviderConnection.getNodes().get(0)).getId())
        .isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProviderConnection.getNodes().get(0)).getName())
        .isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProviderConnection.getNodes().get(0))
                   .isContinuousEfficiencyEnabled())
        .isFalse();
  }
}
