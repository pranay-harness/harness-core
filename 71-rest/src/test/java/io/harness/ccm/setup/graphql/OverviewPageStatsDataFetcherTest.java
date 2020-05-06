package io.harness.ccm.setup.graphql;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.settings.SettingValue;

public class OverviewPageStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Inject OverviewPageStatsDataFetcher overviewPageStatsDataFetcher;

  private static final String CONNECTOR_NAME =
      "connectorName_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();
  private static final String UUID = "uuid_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetch() {
    QLCEOverviewStatsData data = overviewPageStatsDataFetcher.fetch(null, ACCOUNT1_ID);
    assertThat(data.getCloudConnectorsPresent()).isFalse();

    // Create a Connector
    SettingValue settingValue = CEAwsConfig.builder().build();
    createCEConnector(UUID, ACCOUNT1_ID, CONNECTOR_NAME, settingValue);
    data = overviewPageStatsDataFetcher.fetch(null, ACCOUNT1_ID);
    assertThat(data.getCloudConnectorsPresent()).isTrue();
  }
}