package software.wings.helpers.ext.pcf;

import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class PcfDeploymentManagerUnsupportedTest extends WingsBaseTest {
  private final PcfDeploymentManagerUnsupported deploymentManager = new PcfDeploymentManagerUnsupported();
  private final PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
  @Mock private ExecutionLogCallback mockLogCallback;

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetOrganizations() {
    assertThatThrownBy(() -> deploymentManager.getOrganizations(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetSpacesForOrganization() {
    assertThatThrownBy(() -> deploymentManager.getSpacesForOrganization(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateApplication() {
    assertThatThrownBy(
        () -> deploymentManager.createApplication(PcfCreateApplicationRequestData.builder().build(), mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testResizeApplication() {
    assertThatThrownBy(() -> deploymentManager.resizeApplication(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testDeleteApplication() {
    assertThatThrownBy(() -> deploymentManager.deleteApplication(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testStopApplication() {
    assertThatThrownBy(() -> deploymentManager.stopApplication(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetApplicationByName() {
    assertThatThrownBy(() -> deploymentManager.getApplicationByName(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUnmapRouteMapForApplication() {
    assertThatThrownBy(
        () -> deploymentManager.unmapRouteMapForApplication(pcfRequestConfig, new ArrayList<>(), mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testMapRouteMapForApplication() {
    assertThatThrownBy(
        () -> deploymentManager.mapRouteMapForApplication(pcfRequestConfig, new ArrayList<>(), mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetDeployedServicesWithNonZeroInstances() {
    assertThatThrownBy(() -> deploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, ""))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetPreviousReleases() {
    assertThatThrownBy(() -> deploymentManager.getPreviousReleases(pcfRequestConfig, ""))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetRouteMaps() {
    assertThatThrownBy(() -> deploymentManager.getRouteMaps(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCheckConnectivity() {
    assertThat(deploymentManager.checkConnectivity(PcfConfig.builder().build(), false, false))
        .isEqualTo("FAILED: connection timed out");
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateRouteMap() {
    assertThatThrownBy(() -> deploymentManager.createRouteMap(pcfRequestConfig, "", "", "", true, true, 1))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testPerformConfigureAutoscalar() {
    assertThatThrownBy(()
                           -> deploymentManager.performConfigureAutoscalar(
                               PcfAppAutoscalarRequestData.builder().build(), mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testChangeAutoscalarState() {
    assertThatThrownBy(()
                           -> deploymentManager.changeAutoscalarState(
                               PcfAppAutoscalarRequestData.builder().build(), mockLogCallback, true))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCheckIfAppAutoscalarInstalled() {
    assertThatThrownBy(deploymentManager::checkIfAppAutoscalarInstalled).isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCheckIfAppHasAutoscalarAttached() {
    assertThatThrownBy(()
                           -> deploymentManager.checkIfAppHasAutoscalarAttached(
                               PcfAppAutoscalarRequestData.builder().build(), mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testResolvePcfPluginHome() {
    assertThat(deploymentManager.resolvePcfPluginHome()).isNull();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUpSizeApplicationWithSteadyStateCheck() {
    assertThatThrownBy(() -> deploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testIsActiveApplication() {
    assertThatThrownBy(() -> deploymentManager.isActiveApplication(pcfRequestConfig, mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSetEnvironmentVariableForAppStatus() {
    assertThatThrownBy(
        () -> deploymentManager.setEnvironmentVariableForAppStatus(pcfRequestConfig, true, mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUnsetEnvironmentVariableForAppStatus() {
    assertThatThrownBy(() -> deploymentManager.unsetEnvironmentVariableForAppStatus(pcfRequestConfig, mockLogCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }
}
