package software.wings.utils;

import static io.harness.UrlConnectionMixin.checkIfFileExists;
import static io.harness.rule.OwnerRule.VIKAS;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.TestCase.assertEquals;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.cdn.CdnConfig;

import java.io.IOException;

public class CdnStorageUrlGeneratorTest {
  private static final String CDN_URL = "https://storage-qa.harness.io";

  private CdnConfig cdnConfig;

  @Before
  public void setUp() {
    cdnConfig = new CdnConfig();
    cdnConfig.setUrl(CDN_URL);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
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
    String watcherMetaDataFilePath = "public/%s/%s/watchers/current.version";
    cdnConfig.setWatcherMetaDataFilePath(watcherMetaDataFilePath);
    CdnStorageUrlGenerator cdnStorageUrlGenerator = new CdnStorageUrlGenerator(cdnConfig, isFreeCluster);
    String signedUrl = cdnStorageUrlGenerator.getWatcherMetaDataFileUrl(env);
    assertEquals(cdnConfig.getUrl() + "/"
            + String.format(
                  watcherMetaDataFilePath, env, cdnStorageUrlGenerator.getClusterTypeFolderName(isFreeCluster)),
        signedUrl);
    assertEquals(HTTP_OK, checkIfFileExists(signedUrl));
  }
}
