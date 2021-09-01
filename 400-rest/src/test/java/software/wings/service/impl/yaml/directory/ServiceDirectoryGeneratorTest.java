package software.wings.service.impl.yaml.directory;

import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ServiceDirectoryGeneratorTest {
  private static final String APP_MANIFEST_NAME = "APP_MANIFEST_NAME";

  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private ServiceDirectoryGenerator serviceDirectoryGenerator;

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
}
