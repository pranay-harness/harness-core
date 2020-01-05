package software.wings.integration;

import static io.harness.rule.OwnerRule.BRETT;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.harness.category.element.IntegrationTests;
import io.harness.network.Http;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessExecutor;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 6/21/17.
 */
@Slf4j
public class DelegateIntegrationTest extends BaseIntegrationTest {
  @Override
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDownloadDelegateZipWithWatcher()
      throws IOException, JSONException, TimeoutException, InterruptedException {
    String url = "https://localhost:9090/api/delegates/downloadUrl?accountId=" + accountId;
    String responseString = Http.getUnsafeOkHttpClient(url)
                                .newCall(new okhttp3.Request.Builder()
                                             .addHeader("Authorization", "Bearer " + userToken)
                                             .addHeader("accept", "application/json")
                                             .build())
                                .execute()
                                .body()
                                .string();
    JSONObject jsonResponseObject = new JSONObject(responseString);

    String zipDownloadUrl = jsonResponseObject.getJSONObject("resource").getString("downloadUrl");
    assertThat(zipDownloadUrl).isNotEmpty();

    assertThat(new ProcessExecutor()
                   .command("rm", "-rf", "harness-delegate", "delegate.tar.gz")
                   .readOutput(true)
                   .execute()
                   .getExitValue())
        .isEqualTo(0);

    try (InputStream stream = Http.getResponseStreamFromUrl(zipDownloadUrl, 600, 600)) {
      FileUtils.copyInputStreamToFile(stream, new File("delegate.tar.gz"));
    }

    assertThat(
        new ProcessExecutor().command("tar", "-xzf", "delegate.tar.gz").readOutput(true).execute().getExitValue())
        .isEqualTo(0);

    List<String> scripts = new ProcessExecutor()
                               .command("ls", "harness-delegate")
                               .readOutput(true)
                               .execute()
                               .getOutput()
                               .getLines()
                               .stream()
                               .map(String::trim)
                               .collect(toList());
    assertThat(scripts).hasSize(5).containsOnly("README.txt", "start.sh", "stop.sh", "delegate.sh", "proxy.config");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldRunDelegate() throws IOException, JSONException, TimeoutException, InterruptedException {
    String url = "https://localhost:9090/api/delegates/downloadUrl?accountId=" + accountId;
    String responseString = Http.getUnsafeOkHttpClient(url)
                                .newCall(new okhttp3.Request.Builder()
                                             .addHeader("Authorization", "Bearer " + userToken)
                                             .addHeader("accept", "application/json")
                                             .build())
                                .execute()
                                .body()
                                .string();

    new ProcessExecutor().command("rm", "-rf", "harness-delegate", "delegate.tar.gz").readOutput(true).execute();
    String zipDownloadUrl = new JSONObject(responseString).getJSONObject("resource").getString("downloadUrl");
    try (InputStream stream = Http.getResponseStreamFromUrl(zipDownloadUrl, 600, 600)) {
      FileUtils.copyInputStreamToFile(stream, new File("delegate.tar.gz"));
    }
    new ProcessExecutor()
        .command("/bin/sh", "-c",
            "tar -xzf delegate.tar.gz && cd harness-delegate && sed -i'' 's/doUpgrade: true/doUpgrade: false/' start.sh")
        .readOutput(true)
        .execute()
        .getOutput()
        .getLines()
        .forEach(logger::info);

    assertThat(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.connected, true).asList())
        .hasSize(0); // no delegate registered

    int commandStatus = new ProcessExecutor()
                            .command("/bin/sh", "-c", "cd harness-delegate && ./start.sh")
                            .readOutput(true)
                            .execute()
                            .getExitValue();
    assertThat(commandStatus).isEqualTo(0);

    waitForDelegateToRegisterWithTimeout();

    // shutdown running delegate
    new ProcessExecutor()
        .command("/bin/sh", "-c", "cd harness-delegate && ./stop.sh")
        .readOutput(true)
        .execute()
        .getOutput()
        .getLines()
        .forEach(logger::info);
    waitForDelegateToDeregisterWithTimeout();
    assertThat(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.connected, true).asList())
        .hasSize(0); // no delegate registered

    /* Delegate upgrade.
      1. Clean delegate collection
      2. Download hardcoded test jar with version 1.0.553
      3. Enable auto upgrade
      4. Delete delegate.jar and config-delegate.yml
      4. Let delegate register and upgrade to a different version
     */

    deleteAllDocuments(asList(Delegate.class));

    new ProcessExecutor()
        .command("/bin/sh", "-c",
            "cd harness-delegate && rm delegate.jar config-delegate.yml && sed -i'' 's|REMOTE_DELEGATE_URL=.*|REMOTE_DELEGATE_URL=https://ci.harness.io/storage/wingsdelegates/latest/delegate.jar|' start.sh && sed -i'' 's|doUpgrade: false|doUpgrade: true|' start.sh")
        .readOutput(true)
        .execute()
        .getOutput()
        .getLines()
        .forEach(logger::info);

    commandStatus = new ProcessExecutor()
                        .command("/bin/sh", "-c", "cd harness-delegate && ./start.sh")
                        .readOutput(true)
                        .execute()
                        .getExitValue();
    assertThat(commandStatus).isEqualTo(0);

    waitForDelegateUpgradeWithTimeout();

    new ProcessExecutor()
        .command("/bin/sh", "-c", "cd harness-delegate && ./stop.sh")
        .readOutput(true)
        .execute()
        .getOutput()
        .getLines()
        .forEach(logger::info);
    waitForDelegateToDeregisterWithTimeout();
    assertThat(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.connected, true).asList())
        .hasSize(0); // no delegate registered
  }

  private void waitForDelegateUpgradeWithTimeout() {
    String testBaseVersion = "1.0.555";
    Set<String> registeredDelegateVersions = new LinkedHashSet<>();

    await().with().pollInterval(Duration.ONE_SECOND).timeout(5, TimeUnit.MINUTES).until(() -> {
      List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class).asList();
      logger.info("Delegate found " + delegates.size());
      delegates.forEach(delegate -> {
        logger.info("Delegate version " + delegate.getVersion());
        registeredDelegateVersions.add(delegate.getVersion());
      });
      return registeredDelegateVersions.size() == 2;
    }, CoreMatchers.is(true));

    assertThat(registeredDelegateVersions).hasSize(2); // TODO: make it more robust
  }

  private void waitForDelegateToDeregisterWithTimeout() {
    await().with().pollInterval(Duration.ONE_SECOND).timeout(1, TimeUnit.MINUTES).until(() -> {
      List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class).asList();
      boolean isDelegateDisConnected = delegates.stream().noneMatch(Delegate::isConnected);
      logger.info("isDelegateDisconnected = {}", isDelegateDisConnected);
      return isDelegateDisConnected;
    }, CoreMatchers.is(true));
  }

  private void waitForDelegateToRegisterWithTimeout() {
    await().with().pollInterval(Duration.ONE_SECOND).timeout(5, TimeUnit.MINUTES).until(() -> {
      List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class).asList();
      boolean connected = delegates.stream().anyMatch(Delegate::isConnected);
      logger.info("isDelegateConnected = {}", connected);
      return connected;
    }, CoreMatchers.is(true));
  }
}
