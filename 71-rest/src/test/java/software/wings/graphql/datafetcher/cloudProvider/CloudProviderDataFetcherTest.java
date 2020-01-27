package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLCloudProviderQueryParameters;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;

public class CloudProviderDataFetcherTest extends AbstractDataFetcherTest {
  @Inject CloudProviderDataFetcher cloudProviderDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    createCloudProvider(ACCOUNT1_ID, APP1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1, CLOUD_PROVIDER1_ID_ACCOUNT1);
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
}
