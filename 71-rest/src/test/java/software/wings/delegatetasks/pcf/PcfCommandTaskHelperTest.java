package software.wings.delegatetasks.pcf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.security.encryption.EncryptedDataDetail;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        ".", RELEASE_NAME, file);

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

  @Test
  @Category(UnitTests.class)
  public void testDownsizePreviousReleases() throws Exception {
    PcfCommandDeployRequest request =
        PcfCommandDeployRequest.builder().accountId(ACCOUNT_ID).downsizeAppDetail(null).build();

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    List<PcfServiceData> pcfServiceDataList = new ArrayList<>();
    List<PcfInstanceElement> pcfInstanceElements = new ArrayList<>();

    // No old app exists
    pcfCommandTaskHelper.downsizePreviousReleases(
        request, pcfRequestConfig, executionLogCallback, pcfServiceDataList, 0, pcfInstanceElements);
    verify(pcfDeploymentManager, never()).getApplicationByName(any());

    InstanceDetail instanceDetail0 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("0")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();

    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("1")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();
    // old app exists, but downsize is not required.
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .diskQuota(1)
                                              .id("id")
                                              .name("app")
                                              .instanceDetails(instanceDetail0, instanceDetail1)
                                              .instances(2)
                                              .memoryLimit(1)
                                              .stack("stack")
                                              .runningInstances(2)
                                              .requestedState("RUNNING")
                                              .build();

    ApplicationDetail applicationDetailAfterDownsize = ApplicationDetail.builder()
                                                           .diskQuota(1)
                                                           .id("id")
                                                           .name("app")
                                                           .instanceDetails(instanceDetail0)
                                                           .instances(1)
                                                           .memoryLimit(1)
                                                           .stack("stack")
                                                           .runningInstances(1)
                                                           .requestedState("RUNNING")
                                                           .build();

    request.setDownsizeAppDetail(
        PcfAppSetupTimeDetails.builder().applicationGuid("1").applicationName("app").initialInstanceCount(1).build());
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());

    // For BG, downsize should never happen.
    request.setStandardBlueGreen(true);
    pcfCommandTaskHelper.downsizePreviousReleases(
        request, pcfRequestConfig, executionLogCallback, pcfServiceDataList, 2, pcfInstanceElements);
    verify(pcfDeploymentManager, never()).getApplicationByName(any());

    // exptectedCount = cuurrentCount, no downsize should be called.
    request.setStandardBlueGreen(false);
    pcfCommandTaskHelper.downsizePreviousReleases(
        request, pcfRequestConfig, executionLogCallback, pcfServiceDataList, 2, pcfInstanceElements);
    verify(pcfDeploymentManager, times(1)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, never()).downSize(any(), any(), any(), any(), any());
    assertThat(pcfServiceDataList.size()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getDesiredCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(pcfServiceDataList.get(0).getName()).isEqualTo("app");

    assertThat(pcfInstanceElements.size()).isEqualTo(2);
    assertThat(pcfInstanceElements.get(0).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(0).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("0");
    assertThat(pcfInstanceElements.get(1).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(1).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(1).getInstanceIndex()).isEqualTo("1");

    // Downsize application from 2 to 1
    doReturn(applicationDetailAfterDownsize).when(pcfDeploymentManager).resizeApplication(any());
    pcfInstanceElements.clear();
    pcfServiceDataList.clear();
    pcfCommandTaskHelper.downsizePreviousReleases(
        request, pcfRequestConfig, executionLogCallback, pcfServiceDataList, 1, pcfInstanceElements);
    verify(pcfDeploymentManager, times(2)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, times(1)).downSize(any(), any(), any(), any(), any());
    assertThat(pcfServiceDataList.size()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getDesiredCount()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(pcfServiceDataList.get(0).getName()).isEqualTo("app");

    assertThat(pcfInstanceElements.size()).isEqualTo(1);
    assertThat(pcfInstanceElements.get(0).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(0).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("0");
  }
}
