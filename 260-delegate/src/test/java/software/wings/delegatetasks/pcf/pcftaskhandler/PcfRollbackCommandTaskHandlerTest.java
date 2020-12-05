package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfRollbackCommandTaskHandlerTest extends WingsBaseTest {
  public static final String URL = "URL";
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  @Mock ExecutionLogCallback executionLogCallback;
  @Mock PcfDeploymentManager pcfDeploymentManager;
  @Mock PcfCommandTaskHelper pcfCommandTaskHelper;

  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;

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
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    // map route maps
    PcfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, urls);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, pcfRequestConfig, executionLogCallback);
    assertThat(pcfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).mapRouteMaps(appName, urls, pcfRequestConfig, executionLogCallback);
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
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    PcfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(false, null, null);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, pcfRequestConfig, executionLogCallback);
    verify(pcfCommandTaskHelper, never())
        .mapRouteMaps(anyString(), anyList(), any(PcfRequestConfig.class), any(ExecutionLogCallback.class));
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
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    PcfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, null);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, pcfRequestConfig, executionLogCallback);
    assertThat(pcfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper, never())
        .mapRouteMaps(anyString(), anyList(), any(PcfRequestConfig.class), any(ExecutionLogCallback.class));
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
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    List<String> urls = Arrays.asList("url2");
    PcfCommandRollbackRequest pcfCommandRequest = createPcfCommandRollbackRequest(true, appName, urls);
    pcfRollbackCommandTaskHandler.restoreRoutesForOldApplication(
        pcfCommandRequest, pcfRequestConfig, executionLogCallback);
    assertThat(pcfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).mapRouteMaps(appName, urls, pcfRequestConfig, executionLogCallback);
  }

  private PcfCommandRollbackRequest createPcfCommandRollbackRequest(
      boolean downsizeApps, String appName, List<String> urls) {
    return PcfCommandRollbackRequest.builder()
        .isStandardBlueGreenWorkflow(false)
        .appsToBeDownSized(
            downsizeApps ? Arrays.asList(
                PcfAppSetupTimeDetails.builder().applicationName(appName).initialInstanceCount(1).urls(urls).build())
                         : null)
        .build();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testUnmapRoutesFromNewAppAfterDownsize() throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    // standard BG workflow
    PcfCommandRollbackRequest commandRollbackRequest =
        PcfCommandRollbackRequest.builder().isStandardBlueGreenWorkflow(true).build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, pcfRequestConfig);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());

    // no new applications
    commandRollbackRequest =
        PcfCommandRollbackRequest.builder().isStandardBlueGreenWorkflow(false).newApplicationDetails(null).build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, pcfRequestConfig);
    verify(pcfCommandTaskHelper, never()).unmapExistingRouteMaps(any(), any(), any());

    // empty name of new application details
    commandRollbackRequest = PcfCommandRollbackRequest.builder()
                                 .isStandardBlueGreenWorkflow(false)
                                 .newApplicationDetails(PcfAppSetupTimeDetails.builder().build())
                                 .build();
    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, pcfRequestConfig);
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
        PcfCommandRollbackRequest.builder()
            .isStandardBlueGreenWorkflow(false)
            .newApplicationDetails(PcfAppSetupTimeDetails.builder().applicationName(appName).build())
            .build();

    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, pcfRequestConfig);
    assertThat(pcfRequestConfig.getApplicationName()).isEqualTo(appName);
    verify(pcfCommandTaskHelper).unmapExistingRouteMaps(applicationDetail, pcfRequestConfig, executionLogCallback);
  }
}
