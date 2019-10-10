package software.wings.sm.states.pcf;

import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.pcf.ManifestType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ServiceElement;
import software.wings.beans.FeatureName;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PcfStateHelperTest extends WingsBaseTest {
  private String SERVICE_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: Service";

  private String ENV_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: Environment";

  private String ENV_SERVICE_MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : ((INSTANCES))\n"
      + "  random-route: true\n"
      + "  level: EnvironmentService";

  private String envServiceId = "envServiceId";

  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ServiceResourceService serviceResourceService;
  @InjectMocks @Inject private PcfStateHelper pcfStateHelper;
  @Mock private ExecutionContext context;

  @Before
  public void setup() throws IllegalAccessException {
    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(SERVICE_ID)
                                       .kind(AppManifestKind.K8S_MANIFEST)
                                       .storeType(StoreType.Local)
                                       .build();
    manifest.setUuid("1234");

    when(context.getAppId()).thenReturn(APP_ID);
    when(applicationManifestService.getByServiceId(anyString(), anyString(), any())).thenReturn(manifest);
    when(applicationManifestService.getManifestFileByFileName(anyString(), anyString()))
        .thenReturn(
            ManifestFile.builder().fileName(MANIFEST_YML).fileContent(PcfSetupStateTest.MANIFEST_YAML_CONTENT).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchManifestYmlString() throws Exception {
    when(featureFlagService.isEnabled(FeatureName.PCF_MANIFEST_REDESIGN, ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);
    String yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);

    when(serviceResourceService.getPcfServiceSpecification(APP_ID, SERVICE_ID))
        .thenReturn(PcfServiceSpecification.builder()
                        .serviceId(SERVICE_ID)
                        .manifestYaml(PcfSetupStateTest.MANIFEST_YAML_CONTENT)
                        .build());
    yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMap() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    // Local Service manifest files
    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Local, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, SERVICE_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent(SERVICE_MANIFEST_YML).build()));
    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(SERVICE_MANIFEST_YML);

    // Remote overrides in environment
    ApplicationManifest envApplicationManifest = generateAppManifest(StoreType.Remote, ENV_ID);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, envApplicationManifest);
    GitFetchFilesResult filesResult = GitFetchFilesResult.builder()
                                          .files(Arrays.asList(GitFile.builder().fileContent(ENV_MANIFEST_YML).build()))
                                          .build();
    filesFromMultipleRepo.put(K8sValuesLocation.EnvironmentGlobal.name(), filesResult);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(ENV_MANIFEST_YML);

    // Local Environment Service manifest files
    ApplicationManifest serviceEnvApplicationManifest = generateAppManifest(StoreType.Local, envServiceId);
    appManifestMap.put(K8sValuesLocation.Environment, serviceEnvApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, envServiceId))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent(ENV_SERVICE_MANIFEST_YML).build()));
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isNotEmpty();
    assertThat(finalManifestFilesMap.get(ManifestType.APPLICATION_MANIFEST)).isEqualTo(ENV_SERVICE_MANIFEST_YML);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithNullGitFetchFileResponse() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Remote, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();

    filesFromMultipleRepo.put(K8sValuesLocation.Service.name(), null);
    fetchFilesResult.setFilesFromMultipleRepo(filesFromMultipleRepo);
    finalManifestFilesMap = pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFinalManifestFilesMapWithInvalidContent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = GitFetchFilesFromMultipleRepoResult.builder().build();

    // Local Service manifest files
    ApplicationManifest serviceApplicationManifest = generateAppManifest(StoreType.Local, SERVICE_ID);
    appManifestMap.put(K8sValuesLocation.Service, serviceApplicationManifest);

    when(applicationManifestService.getManifestFilesByAppManifestId(APP_ID, SERVICE_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileContent("abc").build()));
    Map<ManifestType, String> finalManifestFilesMap =
        pcfStateHelper.getFinalManifestFilesMap(appManifestMap, fetchFilesResult);
    assertThat(finalManifestFilesMap).isEmpty();
  }

  private ApplicationManifest generateAppManifest(StoreType storeType, String id) {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(storeType).build();
    applicationManifest.setUuid(id);
    applicationManifest.setAppId(APP_ID);

    return applicationManifest;
  }
}
