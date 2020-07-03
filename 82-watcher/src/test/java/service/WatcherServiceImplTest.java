package service;

import static io.harness.rule.OwnerRule.VUK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.watcher.service.WatcherServiceImpl;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicLong;

public class WatcherServiceImplTest extends CategoryTest {
  private WatcherServiceImpl watcherService = Mockito.spy(WatcherServiceImpl.class);

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldDiskFullFalse() {
    boolean fullDisk = watcherService.isDiskFull();

    assertThat(fullDisk).isFalse();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldDiskFullAfterFreeSpaceFalse() throws IllegalAccessException {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(13L), true);

    when(watcherService.getDiskFreeSpace()).thenReturn(14L);

    boolean fullDisk = watcherService.isDiskFull();

    assertThat(fullDisk).isFalse();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldDiskFullFalseTrue() throws IllegalAccessException {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(13L), true);

    when(watcherService.getDiskFreeSpace()).thenReturn(13L);

    boolean fullDisk = watcherService.isDiskFull();

    assertThat(fullDisk).isTrue();
  }
}
