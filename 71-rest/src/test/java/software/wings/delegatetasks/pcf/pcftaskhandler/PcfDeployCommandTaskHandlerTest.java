package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PcfDeployCommandTaskHandlerTest extends WingsBaseTest {
  @Mock ExecutionLogCallback executionLogCallback;
  @Mock PcfCommandTaskHelper pcfCommandTaskHelper;
  @Mock PcfDeploymentManager pcfDeploymentManager;

  @InjectMocks @Inject PcfDeployCommandTaskHandler pcfDeployCommandTaskHandler;

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroStandardBG() throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    PcfCommandDeployRequest pcfCommandDeployRequest =
        PcfCommandDeployRequest.builder().isStandardBlueGreen(true).build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppDetails() throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    PcfCommandDeployRequest pcfCommandDeployRequest =
        PcfCommandDeployRequest.builder().isStandardBlueGreen(false).downsizeAppDetail(null).build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppName() throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    PcfCommandDeployRequest pcfCommandDeployRequest = PcfCommandDeployRequest.builder()
                                                          .isStandardBlueGreen(false)
                                                          .downsizeAppDetail(PcfAppSetupTimeDetails.builder().build())
                                                          .build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppNameNumberOfInstancesNotZero()
      throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    String appName = "appName";
    PcfCommandDeployRequest pcfCommandDeployRequest =
        PcfCommandDeployRequest.builder()
            .isStandardBlueGreen(false)
            .downsizeAppDetail(PcfAppSetupTimeDetails.builder().applicationName(appName).build())
            .build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .urls(Arrays.asList("url1"))
                                              .runningInstances(1)
                                              .build();
    when(pcfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZero() throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    String appName = "appName";
    PcfCommandDeployRequest pcfCommandDeployRequest =
        PcfCommandDeployRequest.builder()
            .isStandardBlueGreen(false)
            .downsizeAppDetail(PcfAppSetupTimeDetails.builder().applicationName(appName).build())
            .build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .urls(Arrays.asList("url1"))
                                              .runningInstances(1)
                                              .build();
    when(pcfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        pcfCommandDeployRequest, pcfRequestConfig, executionLogCallback);
    assertThat(pcfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).unmapExistingRouteMaps(applicationDetail, pcfRequestConfig, executionLogCallback);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalInvalidArgumentsException() {
    try {
      pcfDeployCommandTaskHandler.executeTaskInternal(
          PcfCommandRollbackRequest.builder().build(), null, executionLogCallback, false);
    } catch (Exception e) {
      assertThatExceptionOfType(InvalidArgumentsException.class);
      InvalidArgumentsException invalidArgumentsException = (InvalidArgumentsException) e;
      assertThat(invalidArgumentsException.getParams())
          .containsValue("pcfCommandRequest: Must be instance of PcfCommandDeployRequest");
    }
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testconfigureAutoscalarIfNeeded() throws PivotalClientApiException, IOException {
    PcfCommandDeployRequest pcfCommandDeployRequest = PcfCommandDeployRequest.builder().build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .urls(Arrays.asList("url1"))
                                              .runningInstances(1)
                                              .build();
    PcfAppAutoscalarRequestData pcfAppAutoscalarRequestData = PcfAppAutoscalarRequestData.builder().build();

    // don't use autoscalar
    pcfCommandDeployRequest.setUseAppAutoscalar(false);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // empty PcfManifestsPackage
    pcfCommandDeployRequest.setUseAppAutoscalar(true);
    pcfCommandDeployRequest.setPcfManifestsPackage(null);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // empty autoscalarManifestsYaml
    pcfCommandDeployRequest.setPcfManifestsPackage(PcfManifestsPackage.builder().build());
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // max count bigger than update count
    pcfCommandDeployRequest.setPcfManifestsPackage(
        PcfManifestsPackage.builder().autoscalarManifestYml("autoscalarManifestYml").build());
    pcfCommandDeployRequest.setMaxCount(2);
    pcfCommandDeployRequest.setUpdateCount(1);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());
  }
}
