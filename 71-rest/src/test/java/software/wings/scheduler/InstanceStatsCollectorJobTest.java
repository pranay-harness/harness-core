package software.wings.scheduler;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.rule.Owner;
import io.harness.scheduler.BackgroundSchedulerLocker;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;

import java.time.Duration;

public class InstanceStatsCollectorJobTest extends CategoryTest {
  public static final String ACCOUNTID = "accountid";
  public static final String APPID_1 = "appid1";

  @Mock private BackgroundSchedulerLocker persistentLocker;
  @Mock private StatsCollector statsCollector;

  @InjectMocks @Inject @Spy InstanceStatsCollectorJob instanceStatsCollectorJob;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createStats() {
    final PersistentLocker locker = mock(PersistentLocker.class);
    doReturn(locker).when(persistentLocker).getLocker();
    doReturn(mock(AcquiredLock.class))
        .when(locker)
        .tryToAcquireLock(any(Class.class), anyString(), any(Duration.class));
    doReturn(true).when(statsCollector).createStats(anyString());
    doReturn(true).when(statsCollector).createServerlessStats(anyString());
    instanceStatsCollectorJob.createStats(ACCOUNTID);
    verify(statsCollector, times(1)).createServerlessStats(ACCOUNTID);
    verify(statsCollector, times(1)).createStats(ACCOUNTID);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createStats_disabled() {
    final PersistentLocker locker = mock(PersistentLocker.class);
    doReturn(locker).when(persistentLocker).getLocker();
    doReturn(mock(AcquiredLock.class))
        .when(locker)
        .tryToAcquireLock(any(Class.class), anyString(), any(Duration.class));
    doReturn(false).when(statsCollector).createStats(anyString());
    doReturn(false).when(statsCollector).createServerlessStats(anyString());
    instanceStatsCollectorJob.createStats(ACCOUNTID);
    verify(statsCollector, times(1)).createServerlessStats(ACCOUNTID);
    verify(statsCollector, times(1)).createStats(ACCOUNTID);
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createStats_nolock() {
    final PersistentLocker locker = mock(PersistentLocker.class);
    doReturn(locker).when(persistentLocker).getLocker();
    doReturn(null).when(locker).tryToAcquireLock(any(Class.class), anyString(), any(Duration.class));
    doReturn(true).when(statsCollector).createStats(anyString());
    doReturn(true).when(statsCollector).createServerlessStats(anyString());
    instanceStatsCollectorJob.createStats(ACCOUNTID);
    verify(statsCollector, times(0)).createServerlessStats(ACCOUNTID);
    verify(statsCollector, times(0)).createStats(ACCOUNTID);
  }
}