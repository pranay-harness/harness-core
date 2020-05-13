package software.wings.delegatetasks.pcf;

import static io.harness.pcf.model.PcfConstants.HOST_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.RANDOM_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.RIHAZ;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.helpers.FileUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.pcf.InvalidPcfStateException;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PcfCommandTaskHelperTest extends WingsBaseTest {
  public static final String MANIFEST_YAML = "  applications:\n"
      + "  - name: ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances: ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      - route: ${ROUTE_MAP}\n";

  public static final String MANIFEST_YAML_LOCAL_EXTENDED = "---\n"
      + "applications:\n"
      + "- name: ${APPLICATION_NAME}\n"
      + "  memory: 350M\n"
      + "  instances: ${INSTANCE_COUNT}\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: ${FILE_LOCATION}\n"
      + "  routes:\n"
      + "  - route: app.harness.io\n"
      + "  - route: stage.harness.io\n";

  public static final String MANIFEST_YAML_LOCAL_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  routes:\n"
      + "  - route: app.harness.io\n"
      + "  - route: stage.harness.io\n";

  public static final String MANIFEST_YAML_LOCAL_WITH_TEMP_ROUTES_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  routes:\n"
      + "  - route: appTemp.harness.io\n"
      + "  - route: stageTemp.harness.io\n";

  public static final String MANIFEST_YAML_RESOLVED_WITH_RANDOM_ROUTE = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  random-route: true\n";

  public static final String MANIFEST_YAML_NO_ROUTE = "  applications:\n"
      + "  - name: ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances: ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    no-route: true\n";

  public static final String MANIFEST_YAML_NO_ROUTE_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  no-route: true\n";

  public static final String MANIFEST_YAML_RANDOM_ROUTE = "  applications:\n"
      + "  - name: ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances: ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    random-route: true\n";

  public static final String MANIFEST_YAML_RANDOM_ROUTE_WITH_HOST = "  applications:\n"
      + "  - name: ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances: ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    random-route: true\n";

  public static final String MANIFEST_YAML_RANDON_ROUTE_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  random-route: true\n";

  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String RUNNING = "RUNNING";
  public static final String APP_ID = "APP_ID";
  public static final String ACTIVITY_ID = "ACTIVITY_ID";
  public static final String REGISTRY_HOST_NAME = "REGISTRY_HOST_NAME";
  public static final String TEST_PATH_NAME = "./test";
  public static final String MANIFEST_YAML_EXTENDED_SUPPORT_REMOTE = "  applications:\n"
      + "  - name : anyName\n"
      + "    memory: 350M\n"
      + "    instances : 2\n"
      + "    buildpacks: \n"
      + "      - dotnet_core_buildpack"
      + "    services:\n"
      + "      - PCCTConfig"
      + "      - PCCTAutoScaler"
      + "    path: /users/location\n"
      + "    routes:\n"
      + "      - route: qa.harness.io\n";
  public static final String MANIFEST_YAML_EXTENDED_SUPPORT_REMOTE_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpacks:\n"
      + "  - dotnet_core_buildpack    services: null\n"
      + "  - PCCTConfig      - PCCTAutoScaler    path: /users/location\n"
      + "  path: /root/app\n"
      + "  routes:\n"
      + "  - route: app.harness.io\n"
      + "  - route: stage.harness.io\n";
  private static final String RELEASE_NAME = "name"
      + "_pcfCommandHelperTest";
  @Mock PcfDeploymentManager pcfDeploymentManager;
  @Mock EncryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock ExecutionLogCallback executionLogCallback;
  @Mock DelegateFileManager delegateFileManager;
  @InjectMocks @Spy PcfCommandTaskHelper pcfCommandTaskHelper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRevisionFromReleaseName() throws Exception {
    Integer revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__1");
    assertThat(1 == revision).isTrue();

    revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__2");
    assertThat(2 == revision).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateManifestVarsYamlFileLocally() throws Exception {
    PcfCreateApplicationRequestData requestData = PcfCreateApplicationRequestData.builder()
                                                      .configPathVar(".")
                                                      .newReleaseName("app" + System.currentTimeMillis())
                                                      .build();

    File f = pcfCommandTaskHelper.createManifestVarsYamlFileLocally(requestData, "a:b", 1);
    assertThat(f).isNotNull();

    BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
    String line;
    StringBuilder stringBuilder = new StringBuilder(128);
    while ((line = bufferedReader.readLine()) != null) {
      stringBuilder.append(line);
    }

    assertThat(stringBuilder.toString()).isEqualTo("a:b");
    pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(f));
    assertThat(f.exists()).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateManifestYamlFileLocally() throws Exception {
    File file = null;

    try {
      file = pcfCommandTaskHelper.createManifestYamlFileLocally(
          PcfCreateApplicationRequestData.builder()
              .finalManifestYaml(MANIFEST_YAML_LOCAL_RESOLVED)
              .setupRequest(PcfCommandSetupRequest.builder()
                                .manifestYaml(MANIFEST_YAML)
                                .routeMaps(Arrays.asList("route1", "route2"))
                                .build())
              .configPathVar(".")
              .newReleaseName(RELEASE_NAME + System.currentTimeMillis())
              .build());

      assertThat(file.exists()).isTrue();

      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      String line;
      StringBuilder stringBuilder = new StringBuilder(128);
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line).append('\n');
      }

      assertThat(stringBuilder.toString()).isEqualTo(MANIFEST_YAML_LOCAL_RESOLVED);
      pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(file));
      assertThat(file.exists()).isFalse();
    } finally {
      if (file != null && file.exists()) {
        FileIo.deleteFileIfExists(file.getAbsolutePath());
      }
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetPrefix() {
    Set<String> names = new HashSet<>();
    names.add("App__Account__dev");

    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Account__dev__1"))).isTrue();
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Login__dev__1"))).isFalse();

    names.clear();
    names.add("App__Login__dev");
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Account__dev__1"))).isFalse();
    assertThat(names.contains(pcfCommandTaskHelper.getAppPrefix("App__Login__dev__1"))).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownSizeListOfInstances() throws Exception {
    reset(pcfDeploymentManager);
    ApplicationDetail detail = ApplicationDetail.builder()
                                   .diskQuota(1)
                                   .id("id")
                                   .name("app")
                                   .instances(0)
                                   .memoryLimit(1)
                                   .stack("stack")
                                   .runningInstances(2)
                                   .requestedState("RUNNING")
                                   .build();

    doReturn(detail).when(pcfDeploymentManager).getApplicationByName(any());
    doReturn(detail).when(pcfDeploymentManager).resizeApplication(any());

    List<PcfServiceData> pcfServiceDataListToBeUpdated = new ArrayList<>();
    List<PcfServiceData> pcfServiceDataList = new ArrayList<>();
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    pcfServiceDataList.add(PcfServiceData.builder().name("test").desiredCount(2).build());
    PcfCommandRollbackRequest commandRollbackRequest =
        PcfCommandRollbackRequest.builder().useAppAutoscalar(true).build();
    String path = EMPTY;

    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfCommandTaskHelper.downSizeListOfInstances(executionLogCallback, pcfServiceDataListToBeUpdated, pcfRequestConfig,
        pcfServiceDataList, commandRollbackRequest,
        PcfAppAutoscalarRequestData.builder()
            .applicationName(detail.getName())
            .applicationGuid(detail.getId())
            .build());
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
    assertThat(pcfServiceDataListToBeUpdated.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownsizePreviousReleases() throws Exception {
    PcfCommandDeployRequest request =
        PcfCommandDeployRequest.builder().accountId(ACCOUNT_ID).downsizeAppDetail(null).build();

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    List<PcfServiceData> pcfServiceDataList = new ArrayList<>();
    List<PcfInstanceElement> pcfInstanceElements = new ArrayList<>();

    // No old app exists
    pcfCommandTaskHelper.downsizePreviousReleases(request, pcfRequestConfig, executionLogCallback, pcfServiceDataList,
        0, pcfInstanceElements, PcfAppAutoscalarRequestData.builder().build());
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
    pcfCommandTaskHelper.downsizePreviousReleases(request, pcfRequestConfig, executionLogCallback, pcfServiceDataList,
        2, pcfInstanceElements, PcfAppAutoscalarRequestData.builder().build());
    verify(pcfDeploymentManager, never()).getApplicationByName(any());

    // exptectedCount = cuurrentCount, no downsize should be called.
    request.setStandardBlueGreen(false);
    pcfCommandTaskHelper.downsizePreviousReleases(request, pcfRequestConfig, executionLogCallback, pcfServiceDataList,
        2, pcfInstanceElements,
        PcfAppAutoscalarRequestData.builder().applicationGuid("id").applicationName("app").build());
    verify(pcfDeploymentManager, times(1)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, never()).downSize(any(), any(), any(), any());
    assertThat(pcfServiceDataList.size()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getDesiredCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(pcfServiceDataList.get(0).getName()).isEqualTo("app");

    // Downsize application from 2 to 1
    doReturn(applicationDetailAfterDownsize).when(pcfDeploymentManager).resizeApplication(any());
    pcfInstanceElements.clear();
    pcfServiceDataList.clear();
    pcfCommandTaskHelper.downsizePreviousReleases(request, pcfRequestConfig, executionLogCallback, pcfServiceDataList,
        1, pcfInstanceElements, PcfAppAutoscalarRequestData.builder().build());
    verify(pcfDeploymentManager, times(2)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, times(1)).downSize(any(), any(), any(), any());
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

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownsizePreviousReleases_autoscalar() throws Exception {
    PcfCommandDeployRequest request =
        PcfCommandDeployRequest.builder().accountId(ACCOUNT_ID).downsizeAppDetail(null).useAppAutoscalar(true).build();

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    List<PcfServiceData> pcfServiceDataList = new ArrayList<>();
    List<PcfInstanceElement> pcfInstanceElements = new ArrayList<>();

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

    // Downsize application from 2 to 1
    doReturn(applicationDetailAfterDownsize).when(pcfDeploymentManager).resizeApplication(any());
    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfInstanceElements.clear();
    pcfServiceDataList.clear();
    pcfCommandTaskHelper.downsizePreviousReleases(request, pcfRequestConfig, executionLogCallback, pcfServiceDataList,
        1, pcfInstanceElements,
        PcfAppAutoscalarRequestData.builder()
            .applicationName(applicationDetail.getName())
            .applicationGuid(applicationDetail.getId())
            .build());
    verify(pcfDeploymentManager, times(1)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, times(1)).downSize(any(), any(), any(), any());
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
    assertThat(pcfServiceDataList.size()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getDesiredCount()).isEqualTo(1);
    assertThat(pcfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(pcfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(pcfServiceDataList.get(0).getName()).isEqualTo("app");
    assertThat(pcfServiceDataList.get(0).isDisableAutoscalarPerformed()).isTrue();

    assertThat(pcfInstanceElements.size()).isEqualTo(1);
    assertThat(pcfInstanceElements.get(0).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(0).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("0");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateManifestYamlForPush() throws Exception {
    List<String> routes = Arrays.asList("app.harness.io", "stage.harness.io");
    List<String> tempRoutes = Arrays.asList("appTemp.harness.io", "stageTemp.harness.io");

    PcfCommandSetupRequest pcfCommandSetupRequest =
        PcfCommandSetupRequest.builder().routeMaps(routes).manifestYaml(MANIFEST_YAML).build();
    pcfCommandSetupRequest.setArtifactStreamAttributes(ArtifactStreamAttributes.builder().build());
    PcfCreateApplicationRequestData requestData = generatePcfCreateApplicationRequestData(pcfCommandSetupRequest);

    // 1. Replace ${ROUTE_MAP with routes from setupRequest}
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(routes);
    String finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_RESOLVED);

    // 2. Replace ${ROUTE_MAP with routes from setupRequest}
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_WITH_TEMP_ROUTES_RESOLVED);

    // 3. Simulation of BG, manifest contains final routes, but they should be replaced with tempRoutes,
    // which are mentioned in PcfSetupRequest
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_LOCAL_EXTENDED);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_WITH_TEMP_ROUTES_RESOLVED);

    // 4. Manifest contains no-route = true, ignore routes in setupRequest
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_NO_ROUTE);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_NO_ROUTE_RESOLVED);

    // 5. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RESOLVED_WITH_RANDOM_ROUTE);

    // 6. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(emptyList());
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RESOLVED_WITH_RANDOM_ROUTE);

    // 7. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_RANDOM_ROUTE);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RANDON_ROUTE_RESOLVED);

    // 8. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_RANDOM_ROUTE_WITH_HOST);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RANDON_ROUTE_RESOLVED);

    // 9
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_EXTENDED_SUPPORT_REMOTE);
    pcfCommandSetupRequest.setRouteMaps(routes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(pcfCommandSetupRequest, requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_EXTENDED_SUPPORT_REMOTE_RESOLVED);
  }

  private PcfCreateApplicationRequestData generatePcfCreateApplicationRequestData(
      PcfCommandSetupRequest pcfCommandSetupRequest) {
    return PcfCreateApplicationRequestData.builder()
        .setupRequest(pcfCommandSetupRequest)
        .newReleaseName("app1__1")
        .artifactPath("/root/app")
        .pcfRequestConfig(PcfRequestConfig.builder().spaceName("space").build())
        .build();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleManifestWithNoRoute() {
    Map map = new HashMap<>();
    map.put(ROUTES_MANIFEST_YML_ELEMENT, new Object());
    pcfCommandTaskHelper.handleManifestWithNoRoute(map, false);
    assertThat(map.containsKey(ROUTES_MANIFEST_YML_ELEMENT)).isFalse();

    try {
      pcfCommandTaskHelper.handleManifestWithNoRoute(map, true);
      fail("Exception was expected, as no-route cant be used with BG");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Config. \"no-route\" can not be used with BG deployment");
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleRandomRouteScenario() {
    Map map = new HashMap<>();
    PcfCreateApplicationRequestData requestData = generatePcfCreateApplicationRequestData(null);

    pcfCommandTaskHelper.handleRandomRouteScenario(requestData, map);
    assertThat(map.containsKey(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)).isTrue();
    assertThat((boolean) map.get(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)).isEqualTo(true);
    assertThat((String) map.get(HOST_MANIFEST_YML_ELEMENT)).isEqualTo("app1-space");

    map.put(HOST_MANIFEST_YML_ELEMENT, "myHost");
    pcfCommandTaskHelper.handleRandomRouteScenario(requestData, map);
    assertThat((boolean) map.get(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)).isEqualTo(true);
    assertThat((String) map.get(HOST_MANIFEST_YML_ELEMENT)).isEqualTo("myHost");
    assertThat(map.containsKey(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateYamlFileLocally() throws Exception {
    String data = "asd";
    File file = pcfCommandTaskHelper.createYamlFileLocally("./test" + System.currentTimeMillis(), data);
    assertThat(file.exists()).isTrue();
    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    assertThat(content).isEqualTo(data);
    FileIo.deleteFileIfExists(file.getAbsolutePath());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateDownsizeDetails() {
    List<PcfAppSetupTimeDetails> details =
        pcfCommandTaskHelper.generateDownsizeDetails(ApplicationSummary.builder()
                                                         .name("a_s_e__4")
                                                         .diskQuota(1)
                                                         .requestedState(RUNNING)
                                                         .id("1")
                                                         .urls(new String[] {"url1", "url2"})
                                                         .instances(2)
                                                         .memoryLimit(1)
                                                         .runningInstances(0)
                                                         .build());
    assertThat(details).isNotNull();
    assertThat(details.size()).isEqualTo(1);
    assertThat(details.get(0).getApplicationName()).isEqualTo("a_s_e__4");
    assertThat(details.get(0).getUrls()).containsExactly("url1", "url2");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFindCurrentActiveApplication() throws Exception {
    ApplicationSummary currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(null, null, null);
    assertThat(currentActiveApplication).isNull();

    doReturn(false).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases = Arrays.asList(ApplicationSummary.builder()
                                                                        .name("a_s_e__4")
                                                                        .diskQuota(1)
                                                                        .requestedState(RUNNING)
                                                                        .id("1")
                                                                        .urls(new String[] {"url1", "url2"})
                                                                        .instances(2)
                                                                        .memoryLimit(1)
                                                                        .runningInstances(0)
                                                                        .build(),
        ApplicationSummary.builder()
            .name("a_s_e__5")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url3", "url4"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(
        previousReleases, PcfRequestConfig.builder().build(), executionLogCallback);
    assertThat(currentActiveApplication).isNotNull();
    assertThat(currentActiveApplication.getName()).isEqualTo("a_s_e__5");
    assertThat(currentActiveApplication.getUrls()).containsExactly("url3", "url4");

    doReturn(true).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases1 = Arrays.asList(ApplicationSummary.builder()
                                                                         .name("a_s_e__6")
                                                                         .diskQuota(1)
                                                                         .requestedState(RUNNING)
                                                                         .id("1")
                                                                         .urls(new String[] {"url5", "url6"})
                                                                         .instances(2)
                                                                         .memoryLimit(1)
                                                                         .runningInstances(0)
                                                                         .build(),
        ApplicationSummary.builder()
            .name("a_s_e__7")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url7", "url8"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    assertThatThrownBy(()
                           -> pcfCommandTaskHelper.findCurrentActiveApplication(
                               previousReleases1, PcfRequestConfig.builder().build(), executionLogCallback))
        .isInstanceOf(InvalidPcfStateException.class);

    doReturn(false).doReturn(true).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases2 = Arrays.asList(ApplicationSummary.builder()
                                                                         .name("a_s_e__6")
                                                                         .diskQuota(1)
                                                                         .requestedState(RUNNING)
                                                                         .id("1")
                                                                         .urls(new String[] {"url5", "url6"})
                                                                         .instances(2)
                                                                         .memoryLimit(1)
                                                                         .runningInstances(0)
                                                                         .build(),
        ApplicationSummary.builder()
            .name("a_s_e__7")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url7", "url8"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(
        previousReleases2, PcfRequestConfig.builder().build(), executionLogCallback);
    assertThat(currentActiveApplication).isNotNull();
    assertThat(currentActiveApplication.getName()).isEqualTo("a_s_e__6");
    assertThat(currentActiveApplication.getUrls()).containsExactly("url5", "url6");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrintInstanceDetails() throws Exception {
    String output = "Instance Details:\n"
        + "Index: 0\n"
        + "State: RUNNING\n"
        + "Disk Usage: 1\n"
        + "CPU: 0.0\n"
        + "Memory Usage: 1\n"
        + "\n"
        + "Index: 1\n"
        + "State: RUNNING\n"
        + "Disk Usage: 2\n"
        + "CPU: 0.0\n"
        + "Memory Usage: 2\n";
    InstanceDetail detail0 = InstanceDetail.builder()
                                 .cpu(0.0)
                                 .index("0")
                                 .diskQuota(1l)
                                 .diskUsage(1l)
                                 .memoryQuota(1l)
                                 .memoryUsage(1l)
                                 .state("RUNNING")
                                 .build();

    InstanceDetail detail1 = InstanceDetail.builder()
                                 .cpu(0.0)
                                 .index("1")
                                 .diskQuota(2l)
                                 .diskUsage(2l)
                                 .memoryQuota(2l)
                                 .memoryUsage(2l)
                                 .state("RUNNING")
                                 .build();

    pcfCommandTaskHelper.printInstanceDetails(executionLogCallback, Arrays.asList(detail0, detail1));
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(executionLogCallback).saveExecutionLog(captor.capture());
    String val = captor.getValue();
    assertThat(output).isEqualTo(val);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void testDownloadArtifact() throws FileNotFoundException, IOException, ExecutionException {
    ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactName("test-artifact").registryHostName(REGISTRY_HOST_NAME).build();
    PcfCommandSetupRequest pcfCommandSetupRequest = PcfCommandSetupRequest.builder()
                                                        .artifactStreamAttributes(artifactStreamAttributes)
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .activityId(ACTIVITY_ID)
                                                        .commandName(PcfCommandRequest.PcfCommandType.SETUP.name())
                                                        .build();

    String randomToken = Long.toString(System.currentTimeMillis());

    String testFileName = randomToken + pcfCommandSetupRequest.getArtifactStreamAttributes().getArtifactName();

    File workingDirectory = FileUtils.createTmpDir();
    File testArtifactFile = FileUtils.createTempFile(FilenameUtils.getName(testFileName), randomToken);

    when(delegateFileManager.downloadArtifactAtRuntime(pcfCommandSetupRequest.getArtifactStreamAttributes(),
             pcfCommandSetupRequest.getAccountId(), pcfCommandSetupRequest.getAppId(),
             pcfCommandSetupRequest.getActivityId(), pcfCommandSetupRequest.getCommandName(),
             pcfCommandSetupRequest.getArtifactStreamAttributes().getRegistryHostName()))
        .thenReturn(new FileInputStream(testArtifactFile));

    File artifactFile = pcfCommandTaskHelper.downloadArtifact(pcfCommandSetupRequest, workingDirectory);

    assertThat(artifactFile.exists()).isTrue();
  }
}
