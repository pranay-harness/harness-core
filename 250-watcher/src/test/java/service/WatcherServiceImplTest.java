package service;

import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.watcher.service.WatcherServiceImpl;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.TimeLimiter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WatcherServiceImplTest extends CategoryTest {
  @Mock private TimeLimiter timeLimiter;
  @InjectMocks @Spy private WatcherServiceImpl watcherService;

  private static final String TEST_RESOURCE_PATH = "250-watcher/src/test/resources/service/";

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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate-migrated-2.yml"), Charsets.UTF_8);
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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher-migrated-2.yml"), Charsets.UTF_8);
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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate-migrated-1.yml"), Charsets.UTF_8);
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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher-migrated-1.yml"), Charsets.UTF_8);
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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate-no-grpc.yml"), Charsets.UTF_8);
    List<String> configDelegateExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-delegate-migrated-added-grpc.yml"), Charsets.UTF_8);
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
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher-no-grpc.yml"), Charsets.UTF_8);
    List<String> configWatcherExpectedLines =
        FileUtils.readLines(new File(TEST_RESOURCE_PATH + "config-watcher-migrated-added-grpc.yml"), Charsets.UTF_8);
    List<String> resultLines = new ArrayList<>();

    boolean updated = watcherService.updateConfigFileContentsWithNewUrls(
        configWatcherSourceLines, resultLines, "https://app.harness.io/api", "config-watcher.yml");
    assertThat(updated).isTrue();

    String contentsActual = StringUtils.join('\n', resultLines);
    String contentsExpected = StringUtils.join('\n', configWatcherExpectedLines);

    assertThat(contentsActual).isEqualTo(contentsExpected);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDownloadRunScriptsBeforeRestartingDelegateAndWatcherWhenDiskFull() throws IllegalAccessException {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(Long.MAX_VALUE), true);

    boolean downloadSuccesful = watcherService.downloadRunScriptsBeforeRestartingDelegateAndWatcher();

    assertThat(downloadSuccesful).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDownloadRunScriptsBeforeRestartingDelegateAndWatcherWhenNoAvailableDelegateVersions()
      throws IllegalAccessException {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(-1L), true);

    boolean downloadSuccesful = watcherService.downloadRunScriptsBeforeRestartingDelegateAndWatcher();

    assertThat(downloadSuccesful).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDownloadRunScriptsBeforeRestartingDelegateAndWatcherWithDelegateVersions() throws Exception {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(-1L), true);
    DelegateConfiguration delegateConfiguration =
        DelegateConfiguration.builder().delegateVersions(Arrays.asList("1", "2")).build();

    RestResponse<DelegateConfiguration> restResponse =
        RestResponse.Builder.aRestResponse().withResource(delegateConfiguration).build();
    when(timeLimiter.callWithTimeout(any(Callable.class), eq(15L), eq(TimeUnit.SECONDS), eq(true)))
        .thenReturn(restResponse);

    boolean downloadSuccesful = watcherService.downloadRunScriptsBeforeRestartingDelegateAndWatcher();

    assertThat(downloadSuccesful).isTrue();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDownloadRunScriptsBeforeRestartingDelegateAndWatcherWithIOException() throws Exception {
    FieldUtils.writeField(watcherService, "lastAvailableDiskSpace", new AtomicLong(-1L), true);
    DelegateConfiguration delegateConfiguration =
        DelegateConfiguration.builder().delegateVersions(Arrays.asList("1", "2")).build();

    RestResponse<DelegateConfiguration> restResponse =
        RestResponse.Builder.aRestResponse().withResource(delegateConfiguration).build();
    when(timeLimiter.callWithTimeout(any(Callable.class), eq(15L), eq(TimeUnit.SECONDS), eq(true)))
        .thenReturn(restResponse);
    IOException ioException = new IOException("test");
    when(timeLimiter.callWithTimeout(any(Callable.class), eq(1L), eq(TimeUnit.MINUTES), eq(true)))
        .thenThrow(ioException);

    boolean downloadSuccesful = watcherService.downloadRunScriptsBeforeRestartingDelegateAndWatcher();
    assertThat(downloadSuccesful).isFalse();

    when(timeLimiter.callWithTimeout(any(Callable.class), eq(1L), eq(TimeUnit.MINUTES), eq(true)))
        .thenThrow(Exception.class);
    downloadSuccesful = watcherService.downloadRunScriptsBeforeRestartingDelegateAndWatcher();
    assertThat(downloadSuccesful).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testSwitchStorage() {
    try {
      watcherService.switchStorage();
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRestartWatcherToUpgradeJre() {
    try {
      watcherService.restartWatcherToUpgradeJre("openjdk");
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRestartDelegateToUpgradeJre() {
    try {
      FieldUtils.writeField(watcherService, "clock", Clock.systemDefaultZone(), true);
      watcherService.restartDelegateToUpgradeJre("oracle", "openjdk");
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFindExpectedDelegateVersionsShouldReturnNull() throws Exception {
    DelegateConfiguration delegateConfiguration =
        DelegateConfiguration.builder().delegateVersions(Arrays.asList("1", "2")).build();

    RestResponse<DelegateConfiguration> restResponse =
        RestResponse.Builder.aRestResponse().withResource(delegateConfiguration).build();
    when(timeLimiter.callWithTimeout(any(Callable.class), eq(15L), eq(TimeUnit.SECONDS), eq(true)))
        .thenReturn(restResponse)
        .thenReturn(null);

    List<String> expectedDelegateVersions = watcherService.findExpectedDelegateVersions();
    assertThat(expectedDelegateVersions).containsExactlyInAnyOrder("1", "2");

    expectedDelegateVersions = watcherService.findExpectedDelegateVersions();
    assertThat(expectedDelegateVersions).isNull();
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