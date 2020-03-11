package software.wings.utils;

import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.Environment;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public final class ApplicationManifestUtilsTest extends WingsBaseTest {
  @Mock private ExecutionContext context;
  @Mock private ApplicationManifestService applicationManifestService;

  @Inject @InjectMocks private ApplicationManifestUtils applicationManifestUtils;
  private ApplicationManifestUtils applicationManifestUtilsSpy = spy(new ApplicationManifestUtils());

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAppManifestByApplyingHelmChartOverride() {
    ApplicationManifestUtils utils = spy(ApplicationManifestUtils.class);

    ExecutionContext context = mock(ExecutionContext.class);

    ApplicationManifest applicationManifestAtService =
        ApplicationManifest.builder().serviceId("1").kind(K8S_MANIFEST).storeType(Local).envId("2").build();

    doReturn(applicationManifestAtService).when(utils).getApplicationManifestForService(context);
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context)).isNull();

    applicationManifestAtService.setStoreType(HelmChartRepo);
    Map<K8sValuesLocation, ApplicationManifest> manifestMap = new HashMap<>();
    doReturn(manifestMap).when(utils).getOverrideApplicationManifests(context, HELM_CHART_OVERRIDE);
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context)).isEqualTo(applicationManifestAtService);

    applicationManifestAtService.setStoreType(HelmSourceRepo);
    ApplicationManifest applicationManifestAtEnv = ApplicationManifest.builder()
                                                       .serviceId("1")
                                                       .kind(HELM_CHART_OVERRIDE)
                                                       .storeType(HelmSourceRepo)
                                                       .envId("2")
                                                       .build();
    manifestMap.put(Environment, applicationManifestAtEnv);
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context)).isEqualTo(applicationManifestAtEnv);

    applicationManifestAtService.setStoreType(HelmChartRepo);
    try {
      utils.getAppManifestByApplyingHelmChartOverride(context);
      fail("Exception was expected");
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testIsKustomizeSource() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(KustomizeSourceRepo).build();
    doReturn(applicationManifest).when(applicationManifestUtilsSpy).getApplicationManifestForService(context);
    assertThat(applicationManifestUtilsSpy.isKustomizeSource(context)).isTrue();

    applicationManifest = ApplicationManifest.builder().storeType(HelmSourceRepo).build();
    doReturn(applicationManifest).when(applicationManifestUtilsSpy).getApplicationManifestForService(context);
    assertThat(applicationManifestUtilsSpy.isKustomizeSource(context)).isFalse();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testPopulateValuesFilesFromAppManifest() {
    ApplicationManifest appManifest1 = ApplicationManifest.builder().storeType(Local).build();
    appManifest1.setUuid("appManifest1");

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.ServiceOverride, appManifest1);

    when(applicationManifestService.getManifestFileByFileName("appManifest1", values_filename))
        .thenReturn(ManifestFile.builder().build());

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.size()).isEqualTo(0);

    ApplicationManifest appManifest2 = ApplicationManifest.builder().storeType(Local).build();
    appManifest2.setUuid("appManifest2");
    appManifestMap.put(K8sValuesLocation.Environment, appManifest2);
    when(applicationManifestService.getManifestFileByFileName("appManifest2", values_filename))
        .thenReturn(ManifestFile.builder().fileContent("fileContent").build());

    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.size()).isEqualTo(1);
    assertThat(valuesFiles.get(K8sValuesLocation.Environment)).isEqualTo("fileContent");
  }
}
