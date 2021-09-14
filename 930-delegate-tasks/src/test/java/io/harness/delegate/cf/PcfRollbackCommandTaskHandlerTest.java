/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf;

import static io.harness.delegate.cf.CfTestConstants.ACCOUNT_ID;
import static io.harness.delegate.cf.CfTestConstants.APP_ID;
import static io.harness.delegate.cf.CfTestConstants.APP_NAME;
import static io.harness.delegate.cf.CfTestConstants.ORG;
import static io.harness.delegate.cf.CfTestConstants.SPACE;
import static io.harness.delegate.cf.CfTestConstants.STOPPED;
import static io.harness.delegate.cf.CfTestConstants.getPcfConfig;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BOJANA;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfDeployCommandResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class PcfRollbackCommandTaskHandlerTest extends CategoryTest {
  @Mock LogCallback executionLogCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock CfDeploymentManager cfDeploymentManager;
  @Mock SecretDecryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;

  @InjectMocks @Spy PcfCommandTaskBaseHelper pcfCommandTaskHelper;
  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;

  @Before
  public void setUp() {
    doReturn(executionLogCallback).when(logStreamingTaskClient).obtainLogCallback(anyString());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformRollback() throws PivotalClientApiException, IOException {
    CfCommandRequest cfCommandRequest =
        CfCommandRollbackRequest.builder()
            .pcfCommandType(CfCommandRequest.PcfCommandType.ROLLBACK)
            .pcfConfig(getPcfConfig())
            .accountId(ACCOUNT_ID)
            .instanceData(
                Arrays.asList(CfServiceData.builder().name("a_s_e__6").previousCount(2).desiredCount(0).build(),
                    CfServiceData.builder().name("a_s_e__4").previousCount(0).desiredCount(2).build()))
            .resizeStrategy(ResizeStrategy.DOWNSIZE_OLD_FIRST)
            .organization(ORG)
            .space(SPACE)
            .timeoutIntervalInMin(5)
            .newApplicationDetails(
                CfAppSetupTimeDetails.builder().applicationName("a_s_e__6").urls(Collections.emptyList()).build())
            .build();

    doReturn(ApplicationDetail.builder()
                 .id("Guid:a_s_e__6")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__")
                 .requestedState(STOPPED)
                 .stack("")
                 .runningInstances(0)
                 .build())
        .doReturn(ApplicationDetail.builder()
                      .id("Guid:a_s_e__4")
                      .diskQuota(1)
                      .instances(1)
                      .memoryLimit(1)
                      .name("a_s_e__4")
                      .requestedState(STOPPED)
                      .stack("")
                      .runningInstances(0)
                      .build())
        .when(cfDeploymentManager)
        .getApplicationByName(any());

    ApplicationDetail applicationDetailDownsize = ApplicationDetail.builder()
                                                      .id("Guid:a_s_e__6")
                                                      .diskQuota(1)
                                                      .instances(0)
                                                      .memoryLimit(1)
                                                      .name("a_s_e__6")
                                                      .requestedState(STOPPED)
                                                      .stack("")
                                                      .runningInstances(0)
                                                      .build();

    doReturn(ApplicationDetail.builder()
                 .instanceDetails(Arrays.asList(InstanceDetail.builder()
                                                    .cpu(1.0)
                                                    .diskQuota((long) 1.23)
                                                    .diskUsage((long) 1.23)
                                                    .index("0")
                                                    .memoryQuota((long) 1)
                                                    .memoryUsage((long) 1)
                                                    .build(),
                     InstanceDetail.builder()
                         .cpu(1.0)
                         .diskQuota((long) 1.23)
                         .diskUsage((long) 1.23)
                         .index("1")
                         .memoryQuota((long) 1)
                         .memoryUsage((long) 1)
                         .build()))
                 .id("Guid:a_s_e__4")
                 .diskQuota(1)
                 .instances(1)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .requestedState("RUNNING")
                 .stack("")
                 .runningInstances(1)
                 .build())
        .doReturn(applicationDetailDownsize)
        .when(cfDeploymentManager)
        .upsizeApplicationWithSteadyStateCheck(any(), any());

    doReturn(ApplicationDetail.builder()
                 .instanceDetails(Arrays.asList(InstanceDetail.builder()
                                                    .cpu(1.0)
                                                    .diskQuota((long) 1.23)
                                                    .diskUsage((long) 1.23)
                                                    .index("1")
                                                    .memoryQuota((long) 1)
                                                    .memoryUsage((long) 1)
                                                    .build(),
                     InstanceDetail.builder()
                         .cpu(1.0)
                         .diskQuota((long) 1.23)
                         .diskUsage((long) 1.23)
                         .index("0")
                         .memoryQuota((long) 1)
                         .memoryUsage((long) 1)
                         .build()))
                 .id("Guid:a_s_e__4")
                 .diskQuota(1)
                 .requestedState("RUNNING")
                 .instances(1)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .stack("")
                 .runningInstances(1)
                 .build())
        .doReturn(applicationDetailDownsize)
        .when(cfDeploymentManager)
        .resizeApplication(any());

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRollbackCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CfDeployCommandResponse pcfDeployCommandResponse =
        (CfDeployCommandResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfDeployCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfDeployCommandResponse.getPcfInstanceElements()).isNotNull();
    assertThat(pcfDeployCommandResponse.getPcfInstanceElements()).hasSize(2);

    Set<String> pcfInstanceElements = new HashSet<>();
    ((CfDeployCommandResponse) cfCommandExecutionResponse.getPcfCommandResponse())
        .getPcfInstanceElements()
        .forEach(pcfInstanceElement
            -> pcfInstanceElements.add(
                pcfInstanceElement.getApplicationId() + ":" + pcfInstanceElement.getInstanceIndex()));
    assertThat(pcfInstanceElements.contains("Guid:a_s_e__4:0")).isTrue();
    assertThat(pcfInstanceElements.contains("Guid:a_s_e__4:1")).isTrue();

    // Test Exception flow
    doThrow(new IOException("")).when(pcfCommandTaskHelper).generateWorkingDirectoryForDeployment();
    cfCommandExecutionResponse =
        pcfRollbackCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    assertThat(cfCommandExecutionResponse.getErrorMessage()).isEqualTo("IOException: ");
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testEnableAutoscalarIfNeeded() throws PivotalClientApiException {
    reset(cfDeploymentManager);

    CfServiceData cfServiceData = CfServiceData.builder().name(APP_NAME).id(APP_ID).build();
    List<CfServiceData> upsizeList = Collections.singletonList(cfServiceData);

    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData =
        CfAppAutoscalarRequestData.builder().configPathVar("path").build();
    doReturn(true).when(cfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());

    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        emptyList(), pcfAppAutoscalarRequestData, executionLogCallback);
    verify(cfDeploymentManager, never()).changeAutoscalarState(any(), any(), anyBoolean());

    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        upsizeList, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(cfDeploymentManager, never()).changeAutoscalarState(any(), any(), anyBoolean());

    cfServiceData.setDisableAutoscalarPerformed(true);
    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        upsizeList, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(cfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRestoreRoutesForOldApplication() throws PivotalClientApiException {
    String appName = APP_NAME;
    List<String> urls = Collections.singletonList("url1");
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(cfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
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
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(cfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
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
    String appName = APP_NAME;
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    when(cfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
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
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .urls(Collections.singletonList("url1"))
                                              .runningInstances(1)
                                              .build();
    when(cfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();

    List<String> urls = Collections.singletonList("url2");
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
                ? Collections.singletonList(
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
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .urls(Collections.singletonList("url1"))
                                              .runningInstances(1)
                                              .build();
    when(cfDeploymentManager.getApplicationByName(any())).thenReturn(applicationDetail);
    commandRollbackRequest =
        CfCommandRollbackRequest.builder()
            .isStandardBlueGreenWorkflow(false)
            .newApplicationDetails(CfAppSetupTimeDetails.builder().applicationName(appName).build())
            .build();

    pcfRollbackCommandTaskHandler.unmapRoutesFromNewAppAfterDownsize(
        executionLogCallback, commandRollbackRequest, cfRequestConfig);
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo("app1");
    verify(pcfCommandTaskHelper).unmapExistingRouteMaps(applicationDetail, cfRequestConfig, executionLogCallback);
  }
}
