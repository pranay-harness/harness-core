package io.harness.delegate.cf;

import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.request.CfCommandDeployRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class PcfDeployCommandTaskHandlerTest extends CategoryTest {
  @Mock LogCallback executionLogCallback;
  @Mock PcfCommandTaskBaseHelper pcfCommandTaskHelper;
  @Mock CfDeploymentManager pcfDeploymentManager;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks @Inject PcfDeployCommandTaskHandler pcfDeployCommandTaskHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroStandardBG() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    CfCommandDeployRequest cfCommandDeployRequest = CfCommandDeployRequest.builder().isStandardBlueGreen(true).build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        cfCommandDeployRequest, cfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppDetails() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    CfCommandDeployRequest cfCommandDeployRequest =
        CfCommandDeployRequest.builder().isStandardBlueGreen(false).downsizeAppDetail(null).build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        cfCommandDeployRequest, cfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppName() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    CfCommandDeployRequest cfCommandDeployRequest = CfCommandDeployRequest.builder()
                                                        .isStandardBlueGreen(false)
                                                        .downsizeAppDetail(CfAppSetupTimeDetails.builder().build())
                                                        .build();
    pcfDeployCommandTaskHandler.unmapRoutesIfAppDownsizedToZero(
        cfCommandDeployRequest, cfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZeroEmptyAppNameNumberOfInstancesNotZero()
      throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    String appName = "appName";
    CfCommandDeployRequest cfCommandDeployRequest =
        CfCommandDeployRequest.builder()
            .isStandardBlueGreen(false)
            .downsizeAppDetail(CfAppSetupTimeDetails.builder().applicationName(appName).build())
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
        cfCommandDeployRequest, cfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesIfAppDownsizedToZero() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    String appName = "appName";
    CfCommandDeployRequest cfCommandDeployRequest =
        CfCommandDeployRequest.builder()
            .isStandardBlueGreen(false)
            .downsizeAppDetail(CfAppSetupTimeDetails.builder().applicationName(appName).build())
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
        cfCommandDeployRequest, cfRequestConfig, executionLogCallback);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).unmapExistingRouteMaps(applicationDetail, cfRequestConfig, executionLogCallback);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalInvalidArgumentsException() {
    try {
      pcfDeployCommandTaskHandler.executeTaskInternal(
          CfCommandRollbackRequest.builder().build(), null, logStreamingTaskClient, false);
    } catch (Exception e) {
      assertThatExceptionOfType(InvalidArgumentsException.class);
      InvalidArgumentsException invalidArgumentsException = (InvalidArgumentsException) e;
      assertThat(invalidArgumentsException.getParams())
          .containsValue("CfCommandRequest: Must be instance of CfCommandDeployRequest");
    }
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testconfigureAutoscalarIfNeeded() throws PivotalClientApiException, IOException {
    CfCommandDeployRequest cfCommandDeployRequest = CfCommandDeployRequest.builder().build();
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
    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData = CfAppAutoscalarRequestData.builder().build();

    // don't use autoscalar
    cfCommandDeployRequest.setUseAppAutoscalar(false);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        cfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // empty PcfManifestsPackage
    cfCommandDeployRequest.setUseAppAutoscalar(true);
    cfCommandDeployRequest.setPcfManifestsPackage(null);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        cfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // empty autoscalarManifestsYaml
    cfCommandDeployRequest.setPcfManifestsPackage(PcfManifestsPackage.builder().build());
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        cfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // max count bigger than update count
    cfCommandDeployRequest.setPcfManifestsPackage(
        PcfManifestsPackage.builder().autoscalarManifestYml("autoscalarManifestYml").build());
    cfCommandDeployRequest.setMaxCount(2);
    cfCommandDeployRequest.setUpdateCount(1);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        cfCommandDeployRequest, applicationDetail, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());
  }
}
