package io.harness.delegate.cf;

import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class PcfRollbackCommandTaskHandlerTest extends CategoryTest {
  public static final String URL = "URL";
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @Mock LogCallback executionLogCallback;
  @Mock CfDeploymentManager pcfDeploymentManager;
  @Mock PcfCommandTaskBaseHelper pcfCommandTaskHelper;

  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRestoreRoutesForOldApplication() throws PivotalClientApiException {
    String appName = "appName";
    List<String> urls = Arrays.asList("url1");
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(pcfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    // map route maps
    CfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, urls);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, cfRequestConfig, executionLogCallback);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).mapRouteMaps(appName, urls, cfRequestConfig, executionLogCallback);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRestoreRoutesForOldApplicationNoAppsToDownsize() throws PivotalClientApiException {
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(pcfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    CfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(false, null, null);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, cfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never())
        .mapRouteMaps(anyString(), anyList(), any(CfRequestConfig.class), any(LogCallback.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRestoreRoutesForOldApplicationEmptyUrls() throws PivotalClientApiException {
    String appName = "appName";
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(pcfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    CfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, null);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, cfRequestConfig, executionLogCallback);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper, never())
        .mapRouteMaps(anyString(), anyList(), any(CfRequestConfig.class), any(LogCallback.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRestoreRoutesForOldApplication2() throws PivotalClientApiException {
    String appName = "appName";
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
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    List<String> urls = Arrays.asList("url2");
    CfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, urls);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, cfRequestConfig, executionLogCallback);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).mapRouteMaps(appName, urls, cfRequestConfig, executionLogCallback);
  }

  private CfCommandRollbackRequest createPcfCommandRollbackRequest(
      boolean downsizeApps, String appName, List<String> urls) {
    return CfCommandRollbackRequest.builder()
        .isStandardBlueGreenWorkflow(false)
        .appsToBeDownSized(downsizeApps
                ? Arrays.asList(
                    CfAppSetupTimeDetails.builder().applicationName(appName).initialInstanceCount(1).urls(urls).build())
                : null)
        .build();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesFromNewAppAfterDownsize() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    // standard BG workflow
    CfCommandRollbackRequest commandRollbackRequest =
        CfCommandRollbackRequest.builder().isStandardBlueGreenWorkflow(true).build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, cfRequestConfig);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());

    // no new applications
    commandRollbackRequest =
        CfCommandRollbackRequest.builder().isStandardBlueGreenWorkflow(false).newApplicationDetails(null).build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, cfRequestConfig);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());

    // empty name of new application details
    commandRollbackRequest = CfCommandRollbackRequest.builder()
                                 .isStandardBlueGreenWorkflow(false)
                                 .newApplicationDetails(CfAppSetupTimeDetails.builder().build())
                                 .build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, cfRequestConfig);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());

    String appName = "appName";
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
    commandRollbackRequest =
        CfCommandRollbackRequest.builder()
            .isStandardBlueGreenWorkflow(false)
            .newApplicationDetails(CfAppSetupTimeDetails.builder().applicationName(appName).build())
            .build();

    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, cfRequestConfig);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).unmapExistingRouteMaps(applicationDetail, cfRequestConfig, executionLogCallback);
  }
}
