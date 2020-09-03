package service;

import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.watcher.service.WatcherServiceImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigDelegateDefaultFreemium() throws IOException {
    List<String> configDelegateSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate-migrated-2.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configDelegateSourceLines, resultLines, "https://app.harness.io/gratis/api", "config-delegate.yml");

    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configDelegateExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigWatcherDefaultFreemium() throws IOException {
    List<String> configWatcherSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher-migrated-2.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configWatcherSourceLines, resultLines, "https://app.harness.io/gratis/api", "config-watcher.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configWatcherExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigDelegateProd() throws IOException {
    List<String> configDelegateSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate-migrated-1.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configDelegateSourceLines, resultLines, "https://app.harness.io/api", "config-delegate.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configDelegateExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigWatcherProd() throws IOException {
    List<String> configWatcherSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher-migrated-1.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configWatcherSourceLines, resultLines, "https://app.harness.io/api", "config-watcher.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configWatcherExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigDelegateProdMissingConfigs() throws IOException {
    List<String> configDelegateSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate-no-grpc.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-delegate-migrated-added-grpc.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configDelegateSourceLines, resultLines, "https://app.harness.io/api", "config-delegate.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configDelegateExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldMigrateConfigWatcherProdMissingConfigs() throws IOException {
    List<String> configWatcherSourceLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher-no-grpc.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(getFileFromResources("service/config-watcher-migrated-added-grpc.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configWatcherSourceLines, resultLines, "https://app.harness.io/api", "config-watcher.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configWatcherExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  private File getFileFromResources(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();

    URL resource = classLoader.getResource(fileName);
    if (resource == null) {
      throw new IllegalArgumentException("file is not found!");
    } else {
      return new File(resource.getFile());
    }
  }
}
