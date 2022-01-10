/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static io.harness.rule.OwnerRule.ROHIT;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLCloudProviderQueryParameters;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CloudProviderDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String CLOUD_PROVIDER2_NAME = "CLOUD_PROVIDER2_NAME";

  @Inject CloudProviderDataFetcher cloudProviderDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    createCloudProvider(ACCOUNT1_ID, APP1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1);
    createCloudProvider(ACCOUNT1_ID, GLOBAL_APP_ID, CLOUD_PROVIDER2_ID_ACCOUNT1, CLOUD_PROVIDER2_NAME);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCloudProviderDataFetcher() {
    QLCloudProvider qlCloudProvider = cloudProviderDataFetcher.fetch(
        QLCloudProviderQueryParameters.builder().cloudProviderId(CLOUD_PROVIDER1_ID_ACCOUNT1).build(), ACCOUNT1_ID);

    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProvider).getId()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProvider).getName()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProvider).isContinuousEfficiencyEnabled()).isFalse();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void testGetByName() {
    QLCloudProvider qlCloudProvider = cloudProviderDataFetcher.fetch(
        QLCloudProviderQueryParameters.builder().name(CLOUD_PROVIDER2_NAME).build(), ACCOUNT1_ID);

    assertThat(qlCloudProvider.getId()).isEqualTo(CLOUD_PROVIDER2_ID_ACCOUNT1);
    assertThat(((QLPhysicalDataCenterCloudProvider) qlCloudProvider).getName()).isEqualTo(CLOUD_PROVIDER2_NAME);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void testGetByNameForNonExistingName() {
    cloudProviderDataFetcher.fetch(
        QLCloudProviderQueryParameters.builder().name("Non existing name").build(), ACCOUNT1_ID);
  }
}
