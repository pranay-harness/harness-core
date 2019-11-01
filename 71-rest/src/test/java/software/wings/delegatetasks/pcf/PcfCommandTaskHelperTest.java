package software.wings.delegatetasks.pcf;

import static io.harness.pcf.model.PcfConstants.HOST_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.RANDOM_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.security.encryption.EncryptedDataDetail;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
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
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final String RELEASE_NAME = "name"
      + "_pcfCommandHelperTest";

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
    verify(pcfCommandTaskHelper, never()).downSize(any(), any(), any(), any());
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
  @Category(UnitTests.class)
  public void testGenerateManifestYamlForPush() throws Exception {
    List<String> routes = Arrays.asList("app.harness.io", "stage.harness.io");
    List<String> tempRoutes = Arrays.asList("appTemp.harness.io", "stageTemp.harness.io");

    PcfCommandSetupRequest pcfCommandSetupRequest =
        PcfCommandSetupRequest.builder().routeMaps(routes).manifestYaml(MANIFEST_YAML).build();

    PcfCreateApplicationRequestData requestData = generatePcfCreateApplicationRequestData(pcfCommandSetupRequest);

    // 1. Replace ${ROUTE_MAP with routes from setupRequest}
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(routes);
    String finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_RESOLVED);

    // 2. Replace ${ROUTE_MAP with routes from setupRequest}
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_WITH_TEMP_ROUTES_RESOLVED);

    // 3. Simulation of BG, manifest contains final routes, but they should be replaced with tempRoutes,
    // which are mentioned in PcfSetupRequest
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_LOCAL_EXTENDED);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_LOCAL_WITH_TEMP_ROUTES_RESOLVED);

    // 4. Manifest contains no-route = true, ignore routes in setupRequest
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_NO_ROUTE);
    pcfCommandSetupRequest.setRouteMaps(tempRoutes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_NO_ROUTE_RESOLVED);

    // 5. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RESOLVED_WITH_RANDOM_ROUTE);

    // 6. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML);
    pcfCommandSetupRequest.setRouteMaps(emptyList());
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RESOLVED_WITH_RANDOM_ROUTE);

    // 7. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_RANDOM_ROUTE);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RANDON_ROUTE_RESOLVED);

    // 8. use random-route when no-routes are provided.
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_RANDOM_ROUTE_WITH_HOST);
    pcfCommandSetupRequest.setRouteMaps(null);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
    assertThat(finalManifest).isEqualTo(MANIFEST_YAML_RANDON_ROUTE_RESOLVED);

    // 9
    pcfCommandSetupRequest.setManifestYaml(MANIFEST_YAML_EXTENDED_SUPPORT_REMOTE);
    pcfCommandSetupRequest.setRouteMaps(routes);
    finalManifest = pcfCommandTaskHelper.generateManifestYamlForPush(requestData);
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
  @Category(UnitTests.class)
  public void testGenerateDownsizeDetails() throws Exception {
    List<ApplicationSummary> previousReleases = new ArrayList<>();
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__4")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(1)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__5")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    doReturn(previousReleases).when(pcfDeploymentManager).getDeployedServicesWithNonZeroInstances(any(), anyString());

    List<PcfAppSetupTimeDetails> details =
        pcfCommandTaskHelper.generateDownsizeDetails(PcfRequestConfig.builder().build(), "a_s_e__5");
    assertThat(details).isNotNull();
    assertThat(details.size()).isEqualTo(1);
    assertThat(details.get(0).getApplicationName()).isEqualTo("a_s_e__4");
  }
}
