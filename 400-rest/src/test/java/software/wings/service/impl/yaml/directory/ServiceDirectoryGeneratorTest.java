package software.wings.service.impl.yaml.directory;

import static io.harness.beans.PageResponse.PageResponseBuilder;
import static io.harness.rule.OwnerRule.BOGDAN;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.OC_PARAMS;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.yaml.directory.DirectoryNode.NodeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.YamlNode;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServiceDirectoryGeneratorTest {
  private static final String APP_MANIFEST_NAME = "app_manifest_name";
  private static final String APP_NAME = "application_name";
  private static final String APP_ID = "app_id";
  private static final String SERVICE_UUID = "service_uuid";
  private static final String SERVICE_NAME = "service_name";
  private static final String SERVICES_DIRECTORY_NAME = "Services";
  private static final String OC_PARAMS_DIRECTORY_NAME = "OC Params";
  private static final String INDEX_FILE_NAME = "Index.yaml";
  private static final String APPLICATION_PATH = "application_path";

  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ConfigService configService;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private ManifestFileFolderNodeGenerator manifestFileFolderNodeGenerator;

  @Inject @InjectMocks private ServiceDirectoryGenerator serviceDirectoryGenerator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGenerateMultipleApplicationManifestYaml() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .name(APP_MANIFEST_NAME)
            .serviceId(SERVICE_ID)
            .storeType(StoreType.HelmChartRepo)
            .kind(AppManifestKind.K8S_MANIFEST)
            .helmChartConfig(HelmChartConfig.builder().connectorId(SETTING_ID).build())
            .build();
    ApplicationManifest applicationManifest2 =
        ApplicationManifest.builder()
            .name(APP_MANIFEST_NAME + 2)
            .serviceId(SERVICE_ID)
            .storeType(StoreType.HelmChartRepo)
            .kind(AppManifestKind.K8S_MANIFEST)
            .helmChartConfig(HelmChartConfig.builder().connectorId(SETTING_ID).build())
            .build();

    when(applicationManifestService.getManifestsByServiceId(any(), any(), eq(AppManifestKind.K8S_MANIFEST)))
        .thenReturn(Arrays.asList(applicationManifest, applicationManifest2));
    when(featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, ACCOUNT_ID)).thenReturn(true);
    Service service = Service.builder().name(SERVICE_NAME).appId(APP_ID).uuid(SERVICE_ID).build();
    FolderNode folderNode = serviceDirectoryGenerator.generateApplicationManifestNodeForService(
        ACCOUNT_ID, service, new DirectoryPath(SETUP_FOLDER));
    assertThat(folderNode.getChildren()).hasSize(2);
    assertThat(folderNode.getChildren().get(0).getName()).isEqualTo(APP_MANIFEST_NAME + ".yaml");
    assertThat(folderNode.getChildren().get(1).getName()).isEqualTo(APP_MANIFEST_NAME + "2.yaml");
  }

  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void shouldShowOcParamsDirectory() {
    // given
    Service service =
        Service.builder().uuid(SERVICE_UUID).appId(APP_ID).name(SERVICE_NAME).accountId("account_id").build();
    PageResponse<Service> servicePageResponse =
        PageResponseBuilder.aPageResponse().withResponse(Collections.singletonList(service)).build();
    doReturn(servicePageResponse).when(serviceResourceService).list(any(), eq(false), eq(false), eq(false), eq(null));

    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .name(APP_MANIFEST_NAME)
                                                  .serviceId(SERVICE_ID)
                                                  .storeType(StoreType.CUSTOM_OPENSHIFT_TEMPLATE)
                                                  .kind(AppManifestKind.K8S_MANIFEST)
                                                  .build();
    doReturn(applicationManifest)
        .when(applicationManifestService)
        .getAppManifest(eq(service.getAppId()), eq(null), eq(service.getUuid()), eq(K8S_MANIFEST));

    ApplicationManifest ocParamsManifest = ApplicationManifest.builder()
                                               .name(APP_MANIFEST_NAME)
                                               .serviceId(SERVICE_ID)
                                               .storeType(StoreType.CUSTOM_OPENSHIFT_TEMPLATE)
                                               .kind(OC_PARAMS)
                                               .build();
    doReturn(ocParamsManifest)
        .when(applicationManifestService)
        .getByServiceId(eq(service.getAppId()), eq(service.getUuid()), eq(OC_PARAMS));

    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();

    // when
    FolderNode serviceNode = serviceDirectoryGenerator.doServices(
        application, new DirectoryPath(APPLICATION_PATH), true, Sets.newHashSet(SERVICE_UUID));

    // then
    DirectoryPath expectedServicesRootPath = new DirectoryPath(APPLICATION_PATH).add(SERVICES_DIRECTORY_NAME);
    assertThat(serviceNode.getDirectoryPath().getPath()).isEqualTo(expectedServicesRootPath.getPath());

    FolderNode serviceFolderNode = getChildFolderWithName(serviceNode, SERVICE_NAME);
    DirectoryPath expectedServicePath = expectedServicesRootPath.clone().add(SERVICE_NAME);
    assertThat(serviceFolderNode.getDirectoryPath().getPath()).isEqualTo(expectedServicePath.getPath());

    FolderNode ocParamsFolderNode = getChildFolderWithName(serviceFolderNode, OC_PARAMS_DIRECTORY_NAME);
    DirectoryPath expectedOcParamsPath = expectedServicePath.clone().add(OC_PARAMS_DIRECTORY_NAME);
    assertThat(ocParamsFolderNode.getDirectoryPath().getPath()).isEqualTo(expectedOcParamsPath.getPath());

    YamlNode ocParamsIndexNode = getChildYamlWithName(ocParamsFolderNode, INDEX_FILE_NAME);
    DirectoryPath expectedOcParamsIndexPath = expectedOcParamsPath.clone().add(INDEX_FILE_NAME);
    assertThat(ocParamsIndexNode.getDirectoryPath().getPath()).isEqualTo(expectedOcParamsIndexPath.getPath());
  }

  private YamlNode getChildYamlWithName(FolderNode parent, String name) {
    return parent.getChildren()
        .stream()
        .filter(directoryNode -> directoryNode.getName().equals(name))
        .filter(directoryNode -> directoryNode.getType().equals(NodeType.YAML))
        .map(directoryNode -> (YamlNode) directoryNode)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(errorMessage(parent, name, NodeType.YAML)));
  }

  private FolderNode getChildFolderWithName(FolderNode parent, String name) {
    return parent.getChildren()
        .stream()
        .filter(directoryNode -> directoryNode.getName().equals(name))
        .filter(directoryNode -> directoryNode.getType().equals(NodeType.FOLDER))
        .map(directoryNode -> (FolderNode) directoryNode)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(errorMessage(parent, name, NodeType.FOLDER)));
  }

  private String errorMessage(FolderNode parent, String name, NodeType type) {
    return String.format("Cannot find %s child. Parent: %s; Name: %s", type, parent.getDirectoryPath(), name);
  }
}
