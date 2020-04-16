package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.util.Date;

public class SearchSyncHeartbeatTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testSearchSyncHeartbeatRun() {
    WingsPersistence spyWingsPersistence = spy(wingsPersistence);

    Instant instant = Instant.now();
    String lockName = "lockName";
    String uuid = "uuid";

    SearchDistributedLock searchDistributedLock =
        new SearchDistributedLock(lockName, uuid, Date.from(instant), instant.toEpochMilli());
    wingsPersistence.save(searchDistributedLock);

    SearchSyncHeartbeat searchSyncHeartbeat = new SearchSyncHeartbeat(spyWingsPersistence, lockName, uuid);
    searchSyncHeartbeat.run();

    verify(spyWingsPersistence, times(5)).save(any(PersistentEntity.class));
    verify(spyWingsPersistence, times(5)).delete(any(PersistentEntity.class));
  }
}
