package software.wings.service.impl.instance.stats.collector;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.FeatureName;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.instance.ServerlessDashboardService;
import software.wings.service.intfc.instance.stats.ServerlessInstanceStatService;

import java.time.Instant;
import java.util.Collections;

public class StatsCollectorImplTest extends CategoryTest {
  public static final String ACCOUNTID = "accountid";
  public static final String APPID_1 = "appid1";

  @Mock private ServerlessInstanceStatService serverlessInstanceStatService;
  @Mock private ServerlessDashboardService serverlessDashboardService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks @Inject @Spy StatsCollectorImpl statsCollector;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void alignedWith10thMinute() {
    Instant instant = Instant.parse("2018-12-03T10:10:30.00Z");
    Instant aligned = StatsCollectorImpl.alignedWithMinute(instant, 10);
    assertThat(Instant.parse("2018-12-03T10:10:00.00Z")).isEqualTo(aligned);

    Instant instant2 = Instant.parse("2018-12-03T10:12:30.00Z");
    aligned = StatsCollectorImpl.alignedWithMinute(instant2, 10);
    assertThat(Instant.parse("2018-12-03T10:10:00.00Z")).isEqualTo(aligned);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createServerlessStats() {
    doReturn(true).when(featureFlagService).isEnabled(eq(FeatureName.SERVERLESS_DASHBOARD_AWS_LAMBDA), anyString());
    final boolean serverlessStats = setup_createServerlessStats();
    assertThat(serverlessStats).isTrue();
  }

  private boolean setup_createServerlessStats() {
    doReturn(Instant.now().minusSeconds(1000)).when(serverlessInstanceStatService).getLastSnapshotTime(anyString());
    doReturn(Collections.singletonList(getServerlessInstance()))
        .when(serverlessDashboardService)
        .getAppInstancesForAccount(anyString(), anyLong());
    doReturn(true).when(serverlessInstanceStatService).save(any(ServerlessInstanceStats.class));
    return statsCollector.createServerlessStats(ACCOUNTID);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createServerlessStats_nofeature_flag() {
    doReturn(false).when(featureFlagService).isEnabled(eq(FeatureName.SERVERLESS_DASHBOARD_AWS_LAMBDA), anyString());
    final boolean serverlessStats = setup_createServerlessStats();
    assertThat(serverlessStats).isTrue();
    verify(serverlessInstanceStatService, times(0)).getLastSnapshotTime(anyString());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createServerlessStats_no_last_snapshot() {
    doReturn(true).when(featureFlagService).isEnabled(eq(FeatureName.SERVERLESS_DASHBOARD_AWS_LAMBDA), anyString());
    doReturn(null).when(serverlessInstanceStatService).getLastSnapshotTime(anyString());
    doReturn(Collections.singletonList(getServerlessInstance()))
        .when(serverlessDashboardService)
        .getAppInstancesForAccount(anyString(), anyLong());
    doReturn(true).when(serverlessInstanceStatService).save(any(ServerlessInstanceStats.class));
    final boolean serverlessStats = statsCollector.createServerlessStats(ACCOUNTID);
    assertThat(serverlessStats).isTrue();
  }
  private ServerlessInstance getServerlessInstance() {
    return ServerlessInstance.builder()
        .uuid("instanceid")
        .appId(APPID_1)
        .createdAt(Instant.now().toEpochMilli())
        .build();
  }
}
