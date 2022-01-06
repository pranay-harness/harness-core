/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.rule.OwnerRule.ANKIT;

import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.TestCase.assertEquals;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.WingsBaseTest;
import software.wings.cdn.CdnConfig;

import com.google.inject.Inject;
import io.dropwizard.configuration.ConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CdnStorageUrlGeneratorTest extends WingsBaseTest {
  private static final String DELEGATE_JAR_VERSION = "50300";
  private static final String WATCHER_JAR_VERSION = "50100";

  private static final String CDN_URL = "https://qa.harness.io";
  private static final String KEY_NAME = "storage-qa-private";
  private static final String DELEGATE_JAR_PATH = "private/shared/delegates/builds/oracle-8u191/%s/delegate.jar";
  private static final String WATCHER_JAR_BASE_PATH = "public/shared/watchers/builds";
  private static final String WATCHER_JAR_PATH = "oracle-8u191/%s/watcher.jar";
  private static final String WATCHER_METADATA_FILE_PATH = "public/%s/%s/watchers/current.version";

  private CdnStorageUrlGenerator cdnStorageUrlGenerator;
  private CdnConfig cdnConfig;

  @Inject private ScmSecret scmSecret;

  @Before
  public void setUp() throws IOException, ConfigurationException, URISyntaxException {
    cdnConfig = new CdnConfig();
    cdnConfig.setUrl(CDN_URL);
    cdnConfig.setKeyName(KEY_NAME);
    cdnConfig.setKeySecret(scmSecret.decryptToString(new SecretName("cdn_key_secret")));
    cdnConfig.setDelegateJarPath(DELEGATE_JAR_PATH);
    cdnConfig.setWatcherJarBasePath(WATCHER_JAR_BASE_PATH);
    cdnConfig.setWatcherJarPath(WATCHER_JAR_PATH);
    cdnConfig.setWatcherMetaDataFilePath(WATCHER_METADATA_FILE_PATH);

    cdnStorageUrlGenerator = new CdnStorageUrlGenerator(cdnConfig, false);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  @Ignore("We should not have unit tests that make API calls")
  public void testGetDelegateJarUrl() throws IOException {
    String delegateJarUrl = cdnStorageUrlGenerator.getDelegateJarUrl(DELEGATE_JAR_VERSION);

    assertEquals(HTTP_OK, checkIfFileExists(delegateJarUrl));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testWatcherJarBaseUrl() {
    String watcherJarBaseUrl = cdnStorageUrlGenerator.getWatcherJarBaseUrl();

    assertEquals(cdnConfig.getUrl() + "/public/shared/watchers/builds", watcherJarBaseUrl);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  @Ignore("We should not have unit tests that make API calls")
  public void testWatcherJarUrl() throws IOException {
    String watcherJarUrl = cdnStorageUrlGenerator.getWatcherJarUrl(WATCHER_JAR_VERSION);

    assertEquals(HTTP_OK, checkIfFileExists(watcherJarUrl));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  @Ignore("We should not have unit tests that make API calls")
  public void testGetWatcherMetaDataFileUrl() throws IOException {
    String env = "qa";
    assertWatcherMetaDataFileExists(env, false);
    assertWatcherMetaDataFileExists(env, true);
    env = "dev";
    assertWatcherMetaDataFileExists(env, false);
    env = "pr";
    assertWatcherMetaDataFileExists(env, false);
  }

  private void assertWatcherMetaDataFileExists(String env, boolean isFreeCluster) throws IOException {
    CdnStorageUrlGenerator cdnStorageUrlGenerator = new CdnStorageUrlGenerator(cdnConfig, isFreeCluster);
    String watcherMetaDataFileUrl = cdnStorageUrlGenerator.getWatcherMetaDataFileUrl(env);
    assertEquals(HTTP_OK, checkIfFileExists(watcherMetaDataFileUrl));
  }

  static int checkIfFileExists(String fileUrl) throws IOException {
    URL url = new URL(fileUrl);
    HttpURLConnection huc = (HttpURLConnection) url.openConnection();
    huc.setRequestMethod("HEAD");
    return huc.getResponseCode();
  }
}
