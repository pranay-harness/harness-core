package software.wings.utils;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.Environment;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public final class ApplicationManifestUtilsTest extends CategoryTest {
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
}
