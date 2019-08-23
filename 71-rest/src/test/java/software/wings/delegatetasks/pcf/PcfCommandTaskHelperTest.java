package software.wings.delegatetasks.pcf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PcfCommandTaskHelperTest extends WingsBaseTest {
  public static final String MANIFEST_YAML = "  applications:\n"
      + "  - name : ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances : ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      - route: ${ROUTE_MAP}\n"
      + "    serviceName: SERV\n";

  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String RUNNING = "RUNNING";
  private static final String RELEASE_NAME = "name"
      + "_pcfCommandHelperTest";

  public static final String MANIFEST_YAML_1 = "  applications:\n"
      + "  - name : " + RELEASE_NAME + "\n"
      + "    memory: 350M\n"
      + "    instances : 0\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: .\n"
      + "    routes:\n"
      + "      - route: ${ROUTE_MAP}\n"
      + "    serviceName: SERV\n";

  @Mock PcfDeploymentManager pcfDeploymentManager;
  @Mock EncryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock ExecutionLogCallback executionLogCallback;
  @Mock DelegateFileManager delegateFileManager;
  @InjectMocks @Spy PcfCommandTaskHelper pcfCommandTaskHelper;

  @Test
  @Category(UnitTests.class)
  public void testGetRevisionFromReleaseName() throws Exception {
    Integer revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__1");
    assertThat(1 == revision).isTrue();

    revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__2");
    assertThat(2 == revision).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateManifestYamlFileLocally() throws Exception {
    File file = new File("./" + RELEASE_NAME + ".yml");
    doReturn(file).when(pcfCommandTaskHelper).getManifestFile(any(), any());
    doReturn(".").when(pcfCommandTaskHelper).getPcfArtifactDownloadDirPath();

    file = pcfCommandTaskHelper.createManifestYamlFileLocally(PcfCommandSetupRequest.builder()
                                                                  .manifestYaml(MANIFEST_YAML)
                                                                  .routeMaps(Arrays.asList("route1", "route2"))
                                                                  .build(),
        ".", RELEASE_NAME);

    assertThat(file.exists()).isTrue();

    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
    String line;
    StringBuilder stringBuilder = new StringBuilder(128);
    while ((line = bufferedReader.readLine()) != null) {
      stringBuilder.append(line).append('\n');
    }

    assertThat(stringBuilder.toString()).isEqualTo(MANIFEST_YAML_1);
    pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(file));
    assertThat(file.exists()).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPrefix() {
    Set<String> names = new HashSet<>();
    names.add("App__Account__dev__");

    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Account__dev__1"))).isTrue();
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Login__dev__1"))).isFalse();

    names.clear();
    names.add("App__Login__dev__");
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Account__dev__1"))).isFalse();
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Login__dev__1"))).isTrue();
  }
}
