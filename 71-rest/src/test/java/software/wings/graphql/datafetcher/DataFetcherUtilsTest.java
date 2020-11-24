package software.wings.graphql.datafetcher;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataFetcherUtilsTest extends CategoryTest {
  private static final String ACCOUNT_ID = "account_id";
  private static final String SAMPLE_ACCOUNT_ID = "sample_account_id";

  @Mock FeatureFlagService featureFlagService;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock MainConfiguration configuration;
  @Mock Connection connection;
  @Mock Statement statement;
  @Mock ResultSet resultSet;

  @InjectMocks private DataFetcherUtils dataFetcherUtils;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(timeScaleDBService.isValid()).thenReturn(true);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(any())).thenReturn(resultSet);
    when(configuration.getCeSetUpConfig())
        .thenReturn(CESetUpConfig.builder().sampleAccountId(SAMPLE_ACCOUNT_ID).build());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldReturnActualAccountIdIfPSQLDataPresent() throws SQLException {
    when(featureFlagService.isEnabledReloadCache(eq(FeatureName.CE_SAMPLE_DATA_GENERATION), eq(ACCOUNT_ID)))
        .thenReturn(true);
    when(resultSet.next()).thenReturn(true).thenReturn(false);

    assertThat(dataFetcherUtils.fetchSampleAccountIdIfNoClusterData(ACCOUNT_ID)).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldReturnSampleAccountIdIfPSQLDataNotPresent() throws SQLException {
    when(featureFlagService.isEnabledReloadCache(eq(FeatureName.CE_SAMPLE_DATA_GENERATION), eq(ACCOUNT_ID)))
        .thenReturn(true);
    when(resultSet.next()).thenReturn(false);

    assertThat(dataFetcherUtils.fetchSampleAccountIdIfNoClusterData(ACCOUNT_ID)).isEqualTo(SAMPLE_ACCOUNT_ID);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCheckForPSQLDataAbsence() throws SQLException {
    when(resultSet.next()).thenReturn(false);

    assertThat(dataFetcherUtils.isAnyClusterDataPresent(ACCOUNT_ID)).isEqualTo(false);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCheckForPSQLDataPresence() throws SQLException {
    when(resultSet.next()).thenReturn(true).thenReturn(false);

    assertThat(dataFetcherUtils.isAnyClusterDataPresent(ACCOUNT_ID)).isEqualTo(true);
  }
}
