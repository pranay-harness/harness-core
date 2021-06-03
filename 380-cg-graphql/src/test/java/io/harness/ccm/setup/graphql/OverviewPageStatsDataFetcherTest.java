package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.dao.CEMetadataRecordDao;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class OverviewPageStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Spy @InjectMocks OverviewPageStatsDataFetcher overviewPageStatsDataFetcher;
  @Mock CEMetadataRecordDao metadataRecordDao;
  @Mock FeatureFlagService featureFlagService;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetch() {
    doReturn(true).when(overviewPageStatsDataFetcher).getCEEnabledCloudProvider(ACCOUNT1_ID);
    CEMetadataRecord ceMetadataRecord = CEMetadataRecord.builder()
                                            .awsConnectorConfigured(false)
                                            .gcpConnectorConfigured(false)
                                            .applicationDataPresent(true)
                                            .clusterDataConfigured(true)
                                            .azureConnectorConfigured(true)
                                            .build();
    when(metadataRecordDao.getByAccountId(ACCOUNT1_ID)).thenReturn(ceMetadataRecord);
    QLCEOverviewStatsData data = overviewPageStatsDataFetcher.fetch(null, ACCOUNT1_ID);
    assertThat(data.getCloudConnectorsPresent()).isTrue();
    assertThat(data.getAwsConnectorsPresent()).isFalse();
    assertThat(data.getGcpConnectorsPresent()).isFalse();
    assertThat(data.getApplicationDataPresent()).isTrue();
    assertThat(data.getClusterDataPresent()).isTrue();
    assertThat(data.getCeEnabledClusterPresent()).isTrue();
  }
}
