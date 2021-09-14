/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf;

import static io.harness.delegate.cf.CfTestConstants.ACCOUNT_ID;
import static io.harness.delegate.cf.CfTestConstants.ORG;
import static io.harness.delegate.cf.CfTestConstants.RUNNING;
import static io.harness.delegate.cf.CfTestConstants.SPACE;
import static io.harness.delegate.cf.CfTestConstants.getPcfConfig;
import static io.harness.delegate.cf.CfTestConstants.getRouteUpdateRequest;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class PcfRouteUpdateCommandTaskHandlerTest extends CategoryTest {
  @Mock CfDeploymentManager cfDeploymentManager;
  @Mock SecretDecryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock LogCallback executionLogCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks @Spy PcfCommandTaskBaseHelper pcfCommandTaskHelper;
  @InjectMocks @Spy PcfRouteUpdateCommandTaskHandler pcfRouteUpdateCommandTaskHandler;

  @Before
  public void setUp() {
    doReturn(executionLogCallback).when(logStreamingTaskClient).obtainLogCallback(anyString());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformSwapRouteExecute() throws PivotalClientApiException {
    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        CfRouteUpdateRequestConfigData.builder()
            .downsizeOldApplication(false)
            .finalRoutes(Collections.singletonList("a.b.c"))
            .isRollback(true)
            .isStandardBlueGreen(true)
            .existingApplicationDetails(Collections.singletonList(
                CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()))
            .build();
    CfCommandRequest cfCommandRequest = getRouteUpdateRequest(routeUpdateRequestConfigData);

    doNothing().when(pcfCommandTaskHelper).mapRouteMaps(anyString(), anyList(), any(), any());
    doNothing().when(pcfCommandTaskHelper).unmapRouteMaps(anyString(), anyList(), any(), any());
    doReturn(null).when(cfDeploymentManager).upsizeApplicationWithSteadyStateCheck(any(), any());
    doReturn(null).when(cfDeploymentManager).resizeApplication(any());

    // 2 Rollback True, existingApplication : available
    reset(cfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(cfDeploymentManager, times(1)).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 3 Rollback True, existingApplication : unavailable
    reset(cfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(cfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 4 Rollback False, existingApplication : unavailable
    reset(cfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setRollback(false);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(cfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 5  Rollback False, existingApplication : available
    reset(cfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(Collections.singletonList(
        CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()));
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(cfDeploymentManager, times(1)).resizeApplication(any());
    verify(cfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testResizeOldApplications() throws PivotalClientApiException {
    List<CfAppSetupTimeDetails> appSetupTimeDetailsList = Collections.singletonList(
        CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build());
    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData = CfRouteUpdateRequestConfigData.builder()
                                                                      .downsizeOldApplication(false)
                                                                      .finalRoutes(Collections.singletonList("a.b.c"))
                                                                      .isRollback(false)
                                                                      .isStandardBlueGreen(true)
                                                                      .build();

    CfCommandRouteUpdateRequest pcfCommandRequest = CfCommandRouteUpdateRequest.builder()
                                                        .pcfCommandType(CfCommandRequest.PcfCommandType.RESIZE)
                                                        .pcfConfig(getPcfConfig())
                                                        .accountId(ACCOUNT_ID)
                                                        .organization(ORG)
                                                        .space(SPACE)
                                                        .timeoutIntervalInMin(2)
                                                        .pcfCommandType(CfCommandRequest.PcfCommandType.UPDATE_ROUTE)
                                                        .pcfRouteUpdateConfigData(routeUpdateRequestConfigData)
                                                        .useAppAutoscalar(false)
                                                        .build();

    reset(cfDeploymentManager);

    // Existing applications are empty. No op expected
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(cfDeploymentManager, never()).resizeApplication(any());

    // Autoscalar is false, existing app is present
    routeUpdateRequestConfigData.setExistingApplicationDetails(appSetupTimeDetailsList);
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(1)
                                              .instanceDetails(Collections.singletonList(InstanceDetail.builder()
                                                                                             .cpu(1.0)
                                                                                             .diskQuota((long) 1.23)
                                                                                             .diskUsage((long) 1.23)
                                                                                             .index("2")
                                                                                             .memoryQuota((long) 1)
                                                                                             .memoryUsage((long) 1)
                                                                                             .build()))
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState(RUNNING)
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    doReturn(applicationDetail).when(cfDeploymentManager).getApplicationByName(any());
    doReturn(applicationDetail).when(cfDeploymentManager).resizeApplication(any());

    doReturn(true).when(pcfCommandTaskHelper).disableAutoscalar(any(), any());
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(cfDeploymentManager, times(1)).resizeApplication(any());
    verify(pcfCommandTaskHelper, never()).disableAutoscalar(any(), any());

    // Autoscalar is true, existing app present
    pcfCommandRequest.setUseAppAutoscalar(true);
    ArgumentCaptor<CfAppAutoscalarRequestData> argumentCaptor =
        ArgumentCaptor.forClass(CfAppAutoscalarRequestData.class);
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(cfDeploymentManager, times(2)).resizeApplication(any());
    verify(pcfCommandTaskHelper, times(1)).disableAutoscalar(argumentCaptor.capture(), any());
    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData = argumentCaptor.getValue();
    assertThat(pcfAppAutoscalarRequestData.getApplicationName()).isEqualTo("app1");
    assertThat(pcfAppAutoscalarRequestData.getApplicationGuid()).isEqualTo("10");
    assertThat(pcfAppAutoscalarRequestData.getTimeoutInMins()).isEqualTo(2);
    assertThat(pcfAppAutoscalarRequestData.isExpectedEnabled()).isTrue();

    routeUpdateRequestConfigData.setRollback(true);
    doReturn(true).when(cfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, true, "");
    verify(cfDeploymentManager, times(1)).changeAutoscalarState(argumentCaptor.capture(), any(), anyBoolean());
    pcfAppAutoscalarRequestData = argumentCaptor.getValue();
    assertThat(pcfAppAutoscalarRequestData.getApplicationName()).isEqualTo("app1");
    assertThat(pcfAppAutoscalarRequestData.getApplicationGuid()).isEqualTo("10");
    assertThat(pcfAppAutoscalarRequestData.getTimeoutInMins()).isEqualTo(2);
    assertThat(pcfAppAutoscalarRequestData.isExpectedEnabled()).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSwapRouteExecutionNeeded() {
    assertThat(pcfRouteUpdateCommandTaskHandler.swapRouteExecutionNeeded(null)).isFalse();
    assertThat(pcfRouteUpdateCommandTaskHandler.swapRouteExecutionNeeded(
                   CfRouteUpdateRequestConfigData.builder().isRollback(false).build()))
        .isTrue();
    assertThat(pcfRouteUpdateCommandTaskHandler.swapRouteExecutionNeeded(
                   CfRouteUpdateRequestConfigData.builder().isRollback(true).skipRollback(false).build()))
        .isTrue();
    assertThat(pcfRouteUpdateCommandTaskHandler.swapRouteExecutionNeeded(
                   CfRouteUpdateRequestConfigData.builder().isRollback(true).skipRollback(true).build()))
        .isFalse();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveApp() throws PivotalClientApiException {
    reset(cfDeploymentManager);
    String appPrefix = "cf_app";
    String inActiveAppName = appPrefix + "_4";
    String activeAppName = appPrefix + "_5";
    String newAppName = appPrefix + "_6";
    List<String> finalRoutes = Arrays.asList("basicRoute.apps.pcf-harness.com", "shiny-jackal-pa.apps.pcf-harness.com");
    List<String> tempRoutes = Collections.singletonList("tempBg.apps.pcf-harness.com");

    CfAppSetupTimeDetails inActiveApp = CfAppSetupTimeDetails.builder()
                                            .activeApp(false)
                                            .applicationGuid("ca289f74-fdb6-486e-8679-2f91d8ce566e")
                                            .initialInstanceCount(5)
                                            .urls(tempRoutes)
                                            .applicationName(inActiveAppName)
                                            .build();

    List<ApplicationSummary> existingApplicationSummaries =
        getExistingApplicationSummaries(appPrefix, inActiveAppName, activeAppName, newAppName);
    doReturn(existingApplicationSummaries).when(cfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        getRouteUpdateConfigData(appPrefix, activeAppName, finalRoutes, tempRoutes, inActiveApp);

    CfCommandRequest cfCommandRequest = getRouteUpdateRequest(routeUpdateRequestConfigData);

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);

    verifyRollbackBehaviour(
        inActiveAppName, activeAppName, newAppName, finalRoutes, tempRoutes, cfCommandExecutionResponse);
  }

  private void verifyRollbackBehaviour(String inActiveAppName, String activeAppName, String newAppName,
      List<String> finalRoutes, List<String> tempRoutes, CfCommandExecutionResponse cfCommandExecutionResponse)
      throws PivotalClientApiException {
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // verify In Active app was up sized
    verify(cfDeploymentManager).upsizeApplicationWithSteadyStateCheck(any(), any());

    // verify correct routes are mapped to Active & InActive application
    ArgumentCaptor<String> appNameMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> mapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper, times(2))
        .mapRouteMaps(appNameMapRouteCaptor.capture(), mapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWasModified = appNameMapRouteCaptor.getAllValues();
    List<List<String>> routesLists = mapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWasModified).isNotNull();
    assertThat(appWhoseRoutesWasModified.size()).isEqualTo(2);
    assertThat(routesLists).isNotNull();
    assertThat(appWhoseRoutesWasModified.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(routesLists.get(0).containsAll(finalRoutes)).isTrue();
    assertThat(appWhoseRoutesWasModified.get(1).equalsIgnoreCase(inActiveAppName)).isTrue();
    assertThat(routesLists.get(1).containsAll(tempRoutes)).isTrue();

    // verify correct routes are unmapped from Active & New application
    ArgumentCaptor<String> appNameUnMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> unMapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper, times(2))
        .unmapRouteMaps(appNameUnMapRouteCaptor.capture(), unMapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWereRemoved = appNameUnMapRouteCaptor.getAllValues();
    List<List<String>> removedRouteList = unMapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWereRemoved).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.size()).isEqualTo(2);
    assertThat(removedRouteList).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(removedRouteList.get(0).containsAll(tempRoutes)).isTrue();
    assertThat(appWhoseRoutesWereRemoved.get(1).equalsIgnoreCase(newAppName)).isTrue();
    assertThat(removedRouteList.get(1).containsAll(finalRoutes)).isTrue();

    // verify setting of ENV variables for Active & InActive app
    ArgumentCaptor<Boolean> isActiveAppCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(cfDeploymentManager, times(2)).setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
    List<Boolean> isActiveAppValues = isActiveAppCaptor.getAllValues();
    assertThat(isActiveAppValues).isNotNull();
    assertThat(isActiveAppValues.get(0)).isTrue(); // for active app
    assertThat(isActiveAppValues.get(1)).isFalse(); // for inactive app

    // verify un-setting of ENV variables for New app
    ArgumentCaptor<CfRequestConfig> cfRequestConfigArgumentCaptor = ArgumentCaptor.forClass(CfRequestConfig.class);
    verify(cfDeploymentManager).unsetEnvironmentVariableForAppStatus(cfRequestConfigArgumentCaptor.capture(), any());
    CfRequestConfig cfRequestConfig = cfRequestConfigArgumentCaptor.getValue();
    assertThat(cfRequestConfig).isNotNull();
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(newAppName);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveAppNotPresent() throws PivotalClientApiException {
    reset(cfDeploymentManager);
    String appPrefix = "cf_app";
    String inActiveAppName = appPrefix + "_4";
    String activeAppName = appPrefix + "_5";
    String newAppName = appPrefix + "_6";
    List<String> finalRoutes = Arrays.asList("basicRoute.apps.pcf-harness.com", "shiny-jackal-pa.apps.pcf-harness.com");
    List<String> tempRoutes = Collections.singletonList("tempBg.apps.pcf-harness.com");

    List<ApplicationSummary> existingApplicationSummaries =
        getExistingApplicationSummaries(appPrefix, inActiveAppName, activeAppName, newAppName);
    doReturn(existingApplicationSummaries).when(cfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        getRouteUpdateConfigData(appPrefix, activeAppName, finalRoutes, tempRoutes, null);

    CfCommandRequest cfCommandRequest = getRouteUpdateRequest(routeUpdateRequestConfigData);

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verifyRollbackBehaviourInActiveAppNotPresent(
        activeAppName, newAppName, finalRoutes, tempRoutes, cfCommandExecutionResponse);
  }

  private void verifyRollbackBehaviourInActiveAppNotPresent(String activeAppName, String newAppName,
      List<String> finalRoutes, List<String> tempRoutes, CfCommandExecutionResponse cfCommandExecutionResponse)
      throws PivotalClientApiException {
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // verify In Active app was not up sized as there is no In Active app
    verify(cfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());

    // verify correct routes are mapped to Active app only
    ArgumentCaptor<String> appNameMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> mapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper).mapRouteMaps(appNameMapRouteCaptor.capture(), mapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWasModified = appNameMapRouteCaptor.getAllValues();
    List<List<String>> routesLists = mapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWasModified).isNotNull();
    assertThat(appWhoseRoutesWasModified.size()).isEqualTo(1);
    assertThat(routesLists).isNotNull();
    assertThat(appWhoseRoutesWasModified.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(routesLists.get(0).containsAll(finalRoutes)).isTrue();

    // verify correct routes are unmapped to Active & New application
    ArgumentCaptor<String> appNameUnMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> unMapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper, times(2))
        .unmapRouteMaps(appNameUnMapRouteCaptor.capture(), unMapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWereRemoved = appNameUnMapRouteCaptor.getAllValues();
    List<List<String>> removedRouteList = unMapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWereRemoved).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.size()).isEqualTo(2);
    assertThat(removedRouteList).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(removedRouteList.get(0).containsAll(tempRoutes)).isTrue();
    assertThat(appWhoseRoutesWereRemoved.get(1).equalsIgnoreCase(newAppName)).isTrue();
    assertThat(removedRouteList.get(1).containsAll(finalRoutes)).isTrue();

    // verify setting of ENV variables for Active
    ArgumentCaptor<Boolean> isActiveAppCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(cfDeploymentManager).setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
    List<Boolean> isActiveAppValues = isActiveAppCaptor.getAllValues();
    assertThat(isActiveAppValues).isNotNull();
    assertThat(isActiveAppValues.size()).isEqualTo(1);
    assertThat(isActiveAppValues.get(0)).isTrue(); // for active app

    // verify un-setting of ENV variables for New app
    ArgumentCaptor<CfRequestConfig> cfRequestConfigArgumentCaptor = ArgumentCaptor.forClass(CfRequestConfig.class);
    verify(cfDeploymentManager).unsetEnvironmentVariableForAppStatus(cfRequestConfigArgumentCaptor.capture(), any());
    CfRequestConfig cfRequestConfig = cfRequestConfigArgumentCaptor.getValue();
    assertThat(cfRequestConfig).isNotNull();
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(newAppName);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveAppFeatureFlagDisabled() throws PivotalClientApiException {
    reset(cfDeploymentManager);
    String appPrefix = "cf_app";
    String inActiveAppName = appPrefix + "_4";
    String activeAppName = appPrefix + "_5";
    String newAppName = appPrefix + "_6";
    List<String> finalRoutes = Arrays.asList("basicRoute.apps.pcf-harness.com", "shiny-jackal-pa.apps.pcf-harness.com");
    List<String> tempRoutes = Collections.singletonList("tempBg.apps.pcf-harness.com");

    CfAppSetupTimeDetails inActiveApp = CfAppSetupTimeDetails.builder()
                                            .activeApp(false)
                                            .applicationGuid("ca289f74-fdb6-486e-8679-2f91d8ce566e")
                                            .initialInstanceCount(5)
                                            .urls(tempRoutes)
                                            .applicationName(inActiveAppName)
                                            .build();

    List<ApplicationSummary> existingApplicationSummaries =
        getExistingApplicationSummaries(appPrefix, inActiveAppName, activeAppName, newAppName);
    doReturn(existingApplicationSummaries).when(cfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        getRouteUpdateConfigData(appPrefix, activeAppName, finalRoutes, tempRoutes, inActiveApp);
    routeUpdateRequestConfigData.setUpSizeInActiveApp(false);

    CfCommandRequest cfCommandRequest = getRouteUpdateRequest(routeUpdateRequestConfigData);

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verifyRollbackBehaviourFFDisabled(activeAppName, newAppName, finalRoutes, tempRoutes);
  }

  private void verifyRollbackBehaviourFFDisabled(String activeAppName, String newAppName, List<String> finalRoutes,
      List<String> tempRoutes) throws PivotalClientApiException {
    // verify In Active app was not up sized as upSizeInActiveApp is disabled
    verify(cfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());

    // verify correct routes are mapped to Active & New app
    ArgumentCaptor<String> appNameMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> mapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper, times(2))
        .mapRouteMaps(appNameMapRouteCaptor.capture(), mapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWasModified = appNameMapRouteCaptor.getAllValues();
    List<List<String>> routesLists = mapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWasModified).isNotNull();
    assertThat(appWhoseRoutesWasModified.size()).isEqualTo(2);
    assertThat(routesLists).isNotNull();
    assertThat(appWhoseRoutesWasModified.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(routesLists.get(0).containsAll(finalRoutes)).isTrue();
    assertThat(appWhoseRoutesWasModified.get(1).equalsIgnoreCase(newAppName)).isTrue();
    assertThat(routesLists.get(1).containsAll(tempRoutes)).isTrue();

    // verify correct routes are unmapped from Active & New application
    ArgumentCaptor<String> appNameUnMapRouteCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<String>> unMapRoutesCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(pcfCommandTaskHelper, times(2))
        .unmapRouteMaps(appNameUnMapRouteCaptor.capture(), unMapRoutesCaptor.capture(), any(), any());
    List<String> appWhoseRoutesWereRemoved = appNameUnMapRouteCaptor.getAllValues();
    List<List<String>> removedRouteList = unMapRoutesCaptor.getAllValues();
    assertThat(appWhoseRoutesWereRemoved).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.size()).isEqualTo(2);
    assertThat(removedRouteList).isNotNull();
    assertThat(appWhoseRoutesWereRemoved.get(0).equalsIgnoreCase(activeAppName)).isTrue();
    assertThat(removedRouteList.get(0).containsAll(tempRoutes)).isTrue();
    assertThat(appWhoseRoutesWereRemoved.get(1).equalsIgnoreCase(newAppName)).isTrue();
    assertThat(removedRouteList.get(1).containsAll(finalRoutes)).isTrue();

    // verify setting of ENV variables for Active & New app
    ArgumentCaptor<Boolean> isActiveAppCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(cfDeploymentManager, times(2)).setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
    List<Boolean> isActiveAppValues = isActiveAppCaptor.getAllValues();
    assertThat(isActiveAppValues).isNotNull();
    assertThat(isActiveAppValues.get(0)).isTrue(); // for active app
    assertThat(isActiveAppValues.get(1)).isFalse(); // for inactive app
  }

  @NotNull
  private List<ApplicationSummary> getExistingApplicationSummaries(
      String appPrefix, String inActiveAppName, String activeAppName, String newAppName) {
    return Arrays.asList(ApplicationSummary.builder()
                             .id("ca289f74-fdb6-486e-8679-2f91d8ce566e")
                             .name(appPrefix + "_3")
                             .diskQuota(1)
                             .instances(2)
                             .memoryLimit(250)
                             .requestedState("STOPPED")
                             .runningInstances(2)
                             .build(),
        ApplicationSummary.builder()
            .id("ca289f74-fdb6-486e-8679-2f91d8ce566e")
            .name(inActiveAppName)
            .diskQuota(1)
            .instances(2)
            .memoryLimit(250)
            .requestedState(RUNNING)
            .runningInstances(2)
            .build(),
        ApplicationSummary.builder()
            .id("806c5057-10d4-44c1-ba1b-9e56bd5a997f")
            .name(activeAppName)
            .diskQuota(1)
            .instances(2)
            .memoryLimit(250)
            .requestedState(RUNNING)
            .runningInstances(2)
            .build(),
        ApplicationSummary.builder()
            .id("914d10c2-76e4-4467-96c7-688b0be7e8ad")
            .name(newAppName)
            .diskQuota(1)
            .instances(2)
            .memoryLimit(250)
            .requestedState(RUNNING)
            .runningInstances(2)
            .build());
  }

  private CfRouteUpdateRequestConfigData getRouteUpdateConfigData(String appPrefix, String activeAppName,
      List<String> finalRoutes, List<String> tempRoutes, CfAppSetupTimeDetails inActiveApp) {
    return CfRouteUpdateRequestConfigData.builder()
        .downsizeOldApplication(false)
        .finalRoutes(finalRoutes)
        .isRollback(true)
        .isStandardBlueGreen(true)
        .existingApplicationDetails(Collections.singletonList(
            CfAppSetupTimeDetails.builder().applicationName(activeAppName).initialInstanceCount(1).build()))
        .existingApplicationNames(Collections.singletonList(activeAppName))
        .newApplicationName(appPrefix + "_6")
        .upSizeInActiveApp(true)
        .existingInActiveApplicationDetails(inActiveApp)
        .cfAppNamePrefix(appPrefix)
        .finalRoutes(finalRoutes)
        .tempRoutes(tempRoutes)
        .build();
  }
}
