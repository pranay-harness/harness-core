package software.wings.helpers.ext.pcf;

import static io.harness.pcf.model.PcfConstants.HARNESS__ACTIVE__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HARNESS__STAGE__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HARNESS__STATUS__IDENTIFIER;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.pcf.PivotalClientApiException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

public class PivotalDeploymentManagerImplTest extends WingsBaseTest {
  @Mock PcfClientImpl client;
  @Mock ExecutionLogCallback logCallback;
  @InjectMocks @Spy PcfDeploymentManagerImpl deploymentManager;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetOrganizations() throws Exception {
    OrganizationSummary summary1 = OrganizationSummary.builder().id("1").name("org1").build();
    OrganizationSummary summary2 = OrganizationSummary.builder().id("2").name("org2").build();

    when(client.getOrganizations(any())).thenReturn(Arrays.asList(summary1, summary2));
    List<String> orgs = deploymentManager.getOrganizations(null);
    assertThat(orgs).isNotNull();
    assertThat(orgs).containsExactly("org1", "org2");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void getAppPrefixByRemovingNumber() {
    assertThat(StringUtils.EMPTY).isEqualTo(deploymentManager.getAppPrefixByRemovingNumber(null));
    assertThat("a_b_c").isEqualTo(deploymentManager.getAppPrefixByRemovingNumber("a_b_c__4"));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void getMatchesPrefix() {
    ApplicationSummary applicationSummary = ApplicationSummary.builder()
                                                .id("id1")
                                                .name("a__b__c__1")
                                                .diskQuota(1)
                                                .instances(1)
                                                .memoryLimit(1)
                                                .requestedState("RUNNING")
                                                .runningInstances(0)
                                                .build();

    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isTrue();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("A__b__c__1")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();

    assertThat(deploymentManager.matchesPrefix("a__b__C", applicationSummary)).isTrue();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__c__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isTrue();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__c__d__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isFalse();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isFalse();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("BG__1_vars.yml")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("BG", applicationSummary)).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testChangeAutoscalarState() throws Exception {
    reset(client);
    doReturn(false).doReturn(true).when(client).checkIfAppHasAutoscalarWithExpectedState(any(), any());

    doNothing().when(client).changeAutoscalarState(any(), any(), anyBoolean());

    doNothing().when(logCallback).saveExecutionLog(anyString());
    deploymentManager.changeAutoscalarState(PcfAppAutoscalarRequestData.builder().build(), logCallback, true);
    verify(client, never()).changeAutoscalarState(any(), any(), anyBoolean());

    deploymentManager.changeAutoscalarState(PcfAppAutoscalarRequestData.builder().build(), logCallback, true);
    verify(client, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformConfigureAutoscalar() throws Exception {
    reset(client);
    doReturn(false).doReturn(true).when(client).checkIfAppHasAutoscalarAttached(any(), any());
    doNothing().when(client).performConfigureAutoscalar(any(), any());

    doNothing().when(logCallback).saveExecutionLog(anyString());
    deploymentManager.performConfigureAutoscalar(PcfAppAutoscalarRequestData.builder().build(), logCallback);
    verify(client, never()).performConfigureAutoscalar(any(), any());

    deploymentManager.performConfigureAutoscalar(PcfAppAutoscalarRequestData.builder().build(), logCallback);
    verify(client, times(1)).performConfigureAutoscalar(any(), any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testReachedDesiredState() {
    ApplicationDetail applicationDetail = generateApplicationDetail(1, new InstanceDetail[] {});
    assertThat(deploymentManager.reachedDesiredState(applicationDetail, 2)).isFalse();

    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(1.0)
                                         .diskQuota((long) 1.23)
                                         .diskUsage((long) 1.23)
                                         .index("0")
                                         .memoryQuota((long) 1)
                                         .memoryUsage((long) 1)
                                         .state("CRASHED")
                                         .build();

    InstanceDetail instanceDetail2 = InstanceDetail.builder()
                                         .cpu(1.0)
                                         .diskQuota((long) 1.23)
                                         .diskUsage((long) 1.23)
                                         .index("0")
                                         .memoryQuota((long) 1)
                                         .memoryUsage((long) 1)
                                         .state("RUNNING")
                                         .build();

    applicationDetail = generateApplicationDetail(1, new InstanceDetail[] {instanceDetail1});
    assertThat(deploymentManager.reachedDesiredState(applicationDetail, 1)).isFalse();

    applicationDetail = generateApplicationDetail(2, new InstanceDetail[] {instanceDetail1, instanceDetail2});
    assertThat(deploymentManager.reachedDesiredState(applicationDetail, 2)).isFalse();

    applicationDetail = generateApplicationDetail(2, new InstanceDetail[] {instanceDetail2, instanceDetail2});
    assertThat(deploymentManager.reachedDesiredState(applicationDetail, 2)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testUpsizeApplicationWithSteadyStateCheck() throws Exception {
    StartedProcess startedProcess = mock(StartedProcess.class);
    Process process = mock(Process.class);

    doReturn(startedProcess).when(deploymentManager).startTailingLogsIfNeeded(any(), any(), any());
    doReturn(process).when(startedProcess).getProcess();
    doReturn(process).when(process).destroyForcibly();
    doNothing().when(process).destroy();

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().desiredCount(1).timeOutIntervalInMins(1).build();
    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(2.0)
                                         .diskQuota((long) 2.23)
                                         .diskUsage((long) 1.23)
                                         .index("0")
                                         .memoryQuota((long) 2)
                                         .memoryUsage((long) 2)
                                         .state("RUNNING")
                                         .build();
    ApplicationDetail applicationDetail = generateApplicationDetail(1, new InstanceDetail[] {instanceDetail1});
    doReturn(applicationDetail).when(client).getApplicationByName(any());
    doNothing().when(client).scaleApplications(any());
    ApplicationDetail applicationDetail1 =
        deploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, logCallback);
    assertThat(applicationDetail).isEqualTo(applicationDetail1);
    verify(process, times(1)).destroy();

    InstanceDetail instanceDetail2 = InstanceDetail.builder()
                                         .cpu(1.0)
                                         .diskQuota((long) 1.23)
                                         .diskUsage((long) 1.23)
                                         .index("0")
                                         .memoryQuota((long) 1)
                                         .memoryUsage((long) 1)
                                         .state("CRASHED")
                                         .build();

    try {
      reset(startedProcess);
      reset(process);
      applicationDetail = generateApplicationDetail(1, new InstanceDetail[] {instanceDetail2});
      doReturn(applicationDetail).when(client).getApplicationByName(any());
      deploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, logCallback);
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage().contains("Failed to reach steady state")).isTrue();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testStartTailingLogsIfNeeded() throws Exception {
    reset(client);
    StartedProcess startedProcess = mock(StartedProcess.class);
    Process process = mock(Process.class);

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    pcfRequestConfig.setUseCFCLI(true);
    doReturn(startedProcess).when(client).tailLogsForPcf(any(), any());
    // startedProcess = null
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, null);
    verify(client, times(1)).tailLogsForPcf(any(), any());

    reset(client);
    doReturn(startedProcess).when(client).tailLogsForPcf(any(), any());
    doReturn(null).when(startedProcess).getProcess();
    // startedProcess.getProcess() = null
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, startedProcess);
    verify(client, times(1)).tailLogsForPcf(any(), any());

    reset(client);
    doReturn(process).when(startedProcess).getProcess();
    doReturn(false).when(process).isAlive();
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, startedProcess);
    verify(client, times(1)).tailLogsForPcf(any(), any());

    reset(client);
    doReturn(true).when(process).isAlive();
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, startedProcess);
    verify(client, never()).tailLogsForPcf(any(), any());

    reset(client);
    pcfRequestConfig.setUseCFCLI(false);
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, null);
    verify(client, never()).tailLogsForPcf(any(), any());

    reset(client);
    pcfRequestConfig.setUseCFCLI(true);
    doThrow(PivotalClientApiException.class).when(client).tailLogsForPcf(any(), any());
    deploymentManager.startTailingLogsIfNeeded(pcfRequestConfig, logCallback, null);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSetEnvironmentVariableForAppStatus() throws Exception {
    reset(client);
    deploymentManager.setEnvironmentVariableForAppStatus(PcfRequestConfig.builder().build(), true, logCallback);
    ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(client, times(1)).setEnvVariablesForApplication(mapCaptor.capture(), any(), any());
    Map map = mapCaptor.getValue();

    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get(HARNESS__STATUS__IDENTIFIER)).isEqualTo(HARNESS__ACTIVE__IDENTIFIER);

    deploymentManager.setEnvironmentVariableForAppStatus(PcfRequestConfig.builder().build(), false, logCallback);
    verify(client, times(2)).setEnvVariablesForApplication(mapCaptor.capture(), any(), any());
    map = mapCaptor.getValue();

    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get(HARNESS__STATUS__IDENTIFIER)).isEqualTo(HARNESS__STAGE__IDENTIFIER);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testUnsetEnvironmentVariableForAppStatus() throws Exception {
    reset(client);
    Map<String, String> userProvided = new HashMap<>();
    userProvided.put(HARNESS__STATUS__IDENTIFIER, HARNESS__STAGE__IDENTIFIER);
    ApplicationEnvironments applicationEnvironments =
        ApplicationEnvironments.builder().userProvided(userProvided).build();

    doReturn(applicationEnvironments).when(client).getApplicationEnvironmentsByName(any());

    deploymentManager.unsetEnvironmentVariableForAppStatus(PcfRequestConfig.builder().build(), logCallback);
    ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
    verify(client).unsetEnvVariablesForApplication(listCaptor.capture(), any(), any());
    List list = listCaptor.getValue();

    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list).containsExactly(HARNESS__STATUS__IDENTIFIER);
  }

  @Test
  @Owner(developers = ADWAIT, intermittent = true)
  @Category(UnitTests.class)
  public void testdestroyProcess() throws Exception {
    StartedProcess startedProcess = mock(StartedProcess.class);
    Process process = mock(Process.class);

    doReturn(process).when(startedProcess).getProcess();
    doReturn(null).when(startedProcess).getFuture();

    reset(deploymentManager);
    doNothing().when(process).destroy();
    doReturn(false).when(process).isAlive();
    deploymentManager.destroyProcess(startedProcess);
    verify(process, times(1)).destroy();
    verify(process, never()).destroyForcibly();

    reset(process);
    doNothing().when(process).destroy();
    doReturn(true).when(process).isAlive();
    deploymentManager.destroyProcess(startedProcess);
    verify(process, times(1)).destroy();
    verify(process, times(1)).destroyForcibly();

    // Test with Real ProcessExecutor
    ProcessExecutor processExecutor =
        new ProcessExecutor().timeout(2, TimeUnit.MINUTES).command("/bin/sh", "-c", "echo \"\"");

    StartedProcess start = processExecutor.start();
    deploymentManager.destroyProcess(start);
    assertThat(start.getFuture().isDone()).isTrue();
    assertThat(start.getProcess().isAlive()).isFalse();
  }

  private ApplicationDetail generateApplicationDetail(int runningCount, InstanceDetail[] instanceDetails) {
    return ApplicationDetail.builder()
        .id("id")
        .name("app")
        .diskQuota(1)
        .stack("stack")
        .instances(runningCount)
        .memoryLimit(1)
        .requestedState("RUNNING")
        .runningInstances(runningCount)
        .instanceDetails(instanceDetails)
        .build();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetOrganizationsFail() throws Exception {
    doThrow(Exception.class).when(client).getOrganizations(any());
    assertThatThrownBy(() -> deploymentManager.getOrganizations(PcfRequestConfig.builder().build()))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetSpacesForOrganization() throws Exception {
    when(client.getSpacesForOrganization(any())).thenReturn(Arrays.asList("space1", "space2"));
    List<String> spaces = deploymentManager.getSpacesForOrganization(PcfRequestConfig.builder().build());
    assertThat(spaces).isNotNull();
    assertThat(spaces).containsExactly("space1", "space2");
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetSpacesForOrganizationFail() throws Exception {
    doThrow(Exception.class).when(client).getSpacesForOrganization(any());
    assertThatThrownBy(() -> deploymentManager.getSpacesForOrganization(PcfRequestConfig.builder().build()))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetRouteMaps() throws Exception {
    when(client.getRoutesForSpace(any())).thenReturn(Arrays.asList("route1", "route2"));
    List<String> routeMaps = deploymentManager.getRouteMaps(PcfRequestConfig.builder().build());
    assertThat(routeMaps).isNotNull();
    assertThat(routeMaps).containsExactly("route1", "route2");
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetRouteMapsFail() throws Exception {
    doThrow(Exception.class).when(client).getRoutesForSpace(any());
    assertThatThrownBy(() -> deploymentManager.getRouteMaps(PcfRequestConfig.builder().build()))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateApplication() throws Exception {
    String appName = "App_1";
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().applicationName(appName).build();
    PcfCreateApplicationRequestData pcfCreateApplicationRequestData =
        PcfCreateApplicationRequestData.builder().pcfRequestConfig(pcfRequestConfig).build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .name(appName)
                                              .stack("stack")
                                              .diskQuota(1)
                                              .id("1")
                                              .instances(2)
                                              .memoryLimit(512)
                                              .requestedState("running")
                                              .runningInstances(2)
                                              .build();

    doNothing().when(client).pushApplicationUsingManifest(eq(pcfCreateApplicationRequestData), eq(logCallback));
    when(client.getApplicationByName(eq(pcfRequestConfig))).thenReturn(applicationDetail);

    ApplicationDetail application = deploymentManager.createApplication(pcfCreateApplicationRequestData, logCallback);
    assertThat(application).isNotNull();
    assertThat(application.getName()).isEqualTo(appName);
    assertThat(application.getStack()).isEqualTo("stack");
    assertThat(application.getDiskQuota()).isEqualTo(1);
    assertThat(application.getInstances()).isEqualTo(2);
    assertThat(application.getMemoryLimit()).isEqualTo(512);
    assertThat(application.getRunningInstances()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateApplicationPushApplicationUsingManifestFail() throws Exception {
    doThrow(Exception.class).when(client).pushApplicationUsingManifest(any(), any());
    assertThatThrownBy(
        () -> deploymentManager.createApplication(PcfCreateApplicationRequestData.builder().build(), logCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateApplicationGetApplicationByNameFail() throws Exception {
    doThrow(Exception.class).when(client).getApplicationByName(any());
    assertThatThrownBy(
        () -> deploymentManager.createApplication(PcfCreateApplicationRequestData.builder().build(), logCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testResizeApplication() throws Exception {
    String appName = "App_1";
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().applicationName(appName).desiredCount(2).build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .name(appName)
                                              .stack("stack")
                                              .diskQuota(1)
                                              .id("1")
                                              .instances(0)
                                              .memoryLimit(512)
                                              .requestedState("running")
                                              .runningInstances(2)
                                              .build();
    when(client.getApplicationByName(eq(pcfRequestConfig))).thenReturn(applicationDetail);
    ApplicationDetail application = deploymentManager.resizeApplication(pcfRequestConfig);
    assertThat(application).isNotNull();
    assertThat(application.getName()).isEqualTo(appName);

    pcfRequestConfig.setDesiredCount(0);
    application = deploymentManager.resizeApplication(pcfRequestConfig);
    assertThat(application).isNotNull();
    assertThat(application.getName()).isEqualTo(appName);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testResizeApplicationFail() throws Exception {
    doThrow(Exception.class).when(client).scaleApplications(any());
    assertThatThrownBy(() -> deploymentManager.resizeApplication(PcfRequestConfig.builder().build()))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUnmapRouteMapForApplication() throws Exception {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().useCFCLI(true).build();
    List<String> paths = Arrays.asList("path1", "path2");
    deploymentManager.unmapRouteMapForApplication(pcfRequestConfig, paths, logCallback);
    verify(client, times(1)).unmapRoutesForApplicationUsingCli(eq(pcfRequestConfig), eq(paths), eq(logCallback));

    reset(client);
    pcfRequestConfig.setUseCFCLI(false);
    deploymentManager.unmapRouteMapForApplication(pcfRequestConfig, paths, logCallback);
    verify(client, times(1)).unmapRoutesForApplication(eq(pcfRequestConfig), eq(paths));

    reset(client);
    doThrow(Exception.class).when(client).unmapRoutesForApplication(eq(pcfRequestConfig), eq(paths));
    assertThatThrownBy(() -> deploymentManager.unmapRouteMapForApplication(pcfRequestConfig, paths, logCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testMapRouteMapForApplication() throws Exception {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().useCFCLI(true).build();
    List<String> paths = Arrays.asList("path1", "path2");
    deploymentManager.mapRouteMapForApplication(pcfRequestConfig, paths, logCallback);
    verify(client, times(1)).mapRoutesForApplicationUsingCli(eq(pcfRequestConfig), eq(paths), eq(logCallback));

    reset(client);
    pcfRequestConfig.setUseCFCLI(false);
    deploymentManager.mapRouteMapForApplication(pcfRequestConfig, paths, logCallback);
    verify(client, times(1)).mapRoutesForApplication(eq(pcfRequestConfig), eq(paths));

    reset(client);
    doThrow(Exception.class).when(client).mapRoutesForApplication(eq(pcfRequestConfig), eq(paths));
    assertThatThrownBy(() -> deploymentManager.mapRouteMapForApplication(pcfRequestConfig, paths, logCallback))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetDeployedServicesWithNonZeroInstances() throws Exception {
    String prefix = "app";
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(Collections.emptyList());
    List<ApplicationSummary> deployedServicesWithNonZeroInstances =
        deploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);
    assertThat(deployedServicesWithNonZeroInstances).isNotNull();
    assertThat(deployedServicesWithNonZeroInstances.size()).isEqualTo(0);

    reset(client);
    ApplicationSummary appSummary1 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 1, 2);
    ApplicationSummary appSummary2 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 2, 2);
    List<ApplicationSummary> applicationSummaries = Arrays.asList(appSummary1, appSummary2);
    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(applicationSummaries);
    deployedServicesWithNonZeroInstances =
        deploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);
    assertThat(deployedServicesWithNonZeroInstances).isNotNull();
    assertThat(deployedServicesWithNonZeroInstances.size()).isEqualTo(2);

    reset(client);
    ApplicationSummary appSummary3 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 1, 0);
    ApplicationSummary appSummary4 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 2, 2);
    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(Arrays.asList(appSummary3, appSummary4));
    deployedServicesWithNonZeroInstances =
        deploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix);
    assertThat(deployedServicesWithNonZeroInstances).isNotNull();
    assertThat(deployedServicesWithNonZeroInstances.size()).isEqualTo(1);

    reset(client);
    doThrow(Exception.class).when(client).getApplications(eq(pcfRequestConfig));
    assertThatThrownBy(() -> deploymentManager.getDeployedServicesWithNonZeroInstances(pcfRequestConfig, prefix))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testGetPreviousReleases() throws Exception {
    String prefix = "app";
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();

    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(Collections.emptyList());
    List<ApplicationSummary> previousReleasesApplication =
        deploymentManager.getPreviousReleases(pcfRequestConfig, prefix);
    assertThat(previousReleasesApplication).isNotNull();
    assertThat(previousReleasesApplication.size()).isEqualTo(0);

    reset(client);
    ApplicationSummary appSummary1 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 1, 2);
    ApplicationSummary appSummary2 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 2, 2);
    List<ApplicationSummary> applicationSummaries = Arrays.asList(appSummary1, appSummary2);
    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(applicationSummaries);
    previousReleasesApplication = deploymentManager.getPreviousReleases(pcfRequestConfig, prefix);
    assertThat(previousReleasesApplication).isNotNull();
    assertThat(previousReleasesApplication.size()).isEqualTo(2);

    reset(client);
    ApplicationSummary appSummary3 = getApplicationSummary(prefix + PcfDeploymentManagerImpl.DELIMITER + 1, 0);
    ApplicationSummary appSummary4 = getApplicationSummary("filter" + PcfDeploymentManagerImpl.DELIMITER + 2, 2);
    when(client.getApplications(eq(pcfRequestConfig))).thenReturn(Arrays.asList(appSummary3, appSummary4));
    previousReleasesApplication = deploymentManager.getPreviousReleases(pcfRequestConfig, prefix);
    assertThat(previousReleasesApplication).isNotNull();
    assertThat(previousReleasesApplication.size()).isEqualTo(1);

    reset(client);
    doThrow(Exception.class).when(client).getApplications(eq(pcfRequestConfig));
    assertThatThrownBy(() -> deploymentManager.getPreviousReleases(pcfRequestConfig, prefix))
        .isInstanceOf(PivotalClientApiException.class);
  }

  private ApplicationSummary getApplicationSummary(String appName, int instances) {
    return ApplicationSummary.builder()
        .name(appName)
        .diskQuota(1)
        .id("1")
        .memoryLimit(512)
        .requestedState("running")
        .runningInstances(instances)
        .instances(instances)
        .build();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testDeleteApplication() throws Exception {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    deploymentManager.deleteApplication(pcfRequestConfig);
    verify(client, times(1)).deleteApplication(eq(pcfRequestConfig));

    reset(client);
    doThrow(Exception.class).when(client).deleteApplication(eq(pcfRequestConfig));
    assertThatThrownBy(() -> deploymentManager.deleteApplication(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testStopApplication() throws Exception {
    String appName = "app_1";
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().applicationName(appName).build();
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .name(appName)
                                              .stack("stack")
                                              .diskQuota(1)
                                              .id("1")
                                              .instances(2)
                                              .memoryLimit(512)
                                              .requestedState("running")
                                              .runningInstances(2)
                                              .build();
    when(client.getApplicationByName(eq(pcfRequestConfig))).thenReturn(applicationDetail);

    String message = deploymentManager.stopApplication(pcfRequestConfig);
    verify(client, times(1)).stopApplication(eq(pcfRequestConfig));
    assertThat(message.contains(appName)).isEqualTo(true);

    reset(client);
    doThrow(Exception.class).when(client).stopApplication(eq(pcfRequestConfig));
    assertThatThrownBy(() -> deploymentManager.stopApplication(pcfRequestConfig))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCreateRouteMap() throws Exception {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().build();
    String host = "localhost";
    String domain = "harness";
    String path = "/console.pivotal";
    int port = 8080;
    String tcpRouteNonRandomPortPath = domain + ":" + port;

    // tcpRoute without random port
    Optional<Route> route = Optional.of(Route.builder().domain(domain).host(host).id("1").space("test").build());
    when(client.getRouteMap(eq(pcfRequestConfig), eq(tcpRouteNonRandomPortPath))).thenReturn(route);
    String routeMap = deploymentManager.createRouteMap(pcfRequestConfig, host, domain, path, true, false, port);
    assertThat(routeMap).isNotNull();
    assertThat(routeMap.equalsIgnoreCase(tcpRouteNonRandomPortPath)).isEqualTo(true);

    reset(client);
    when(client.getRouteMap(eq(pcfRequestConfig), eq(tcpRouteNonRandomPortPath))).thenReturn(Optional.empty());
    assertThatThrownBy(() -> deploymentManager.createRouteMap(pcfRequestConfig, host, domain, path, true, false, port))
        .isInstanceOf(PivotalClientApiException.class);

    // tcpRoute with RandomPort
    reset(client);
    String tcpRouteRandomPortPath = domain;
    when(client.getRouteMap(eq(pcfRequestConfig), eq(tcpRouteRandomPortPath))).thenReturn(route);
    routeMap = deploymentManager.createRouteMap(pcfRequestConfig, host, domain, path, true, true, null);
    assertThat(routeMap).isNotNull();
    assertThat(routeMap.equalsIgnoreCase(tcpRouteRandomPortPath)).isEqualTo(true);

    // nonTcpRoute with nonBlankPath
    reset(client);
    String nonTcpRouteNonBlankPath = host + "." + domain + path;
    when(client.getRouteMap(eq(pcfRequestConfig), eq(nonTcpRouteNonBlankPath))).thenReturn(route);
    routeMap = deploymentManager.createRouteMap(pcfRequestConfig, host, domain, path, false, true, null);
    assertThat(routeMap).isNotNull();
    assertThat(routeMap.equalsIgnoreCase(nonTcpRouteNonBlankPath)).isEqualTo(true);

    reset(client);
    String emptyDomain = "";
    assertThatThrownBy(
        () -> deploymentManager.createRouteMap(pcfRequestConfig, host, emptyDomain, path, true, false, port))
        .isInstanceOf(PivotalClientApiException.class);

    reset(client);
    String emptyHost = "";
    assertThatThrownBy(
        () -> deploymentManager.createRouteMap(pcfRequestConfig, emptyHost, domain, path, false, false, port))
        .isInstanceOf(PivotalClientApiException.class);

    reset(client);
    assertThatThrownBy(() -> deploymentManager.createRouteMap(pcfRequestConfig, host, domain, path, true, false, null))
        .isInstanceOf(PivotalClientApiException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCheckConnectivity() throws Exception {
    PcfConfig pcfConfig = PcfConfig.builder().username("user".toCharArray()).password("test".toCharArray()).build();
    when(client.getOrganizations(any())).thenReturn(Collections.emptyList());
    String message = deploymentManager.checkConnectivity(pcfConfig, false, false);
    verify(client, times(1)).getOrganizations(any());
    assertThat(message.equalsIgnoreCase("SUCCESS")).isEqualTo(true);

    reset(client);
    doThrow(Exception.class).when(client).getOrganizations(any());
    message = deploymentManager.checkConnectivity(pcfConfig, false, false);
    assertThat(message.equalsIgnoreCase("SUCCESS")).isEqualTo(false);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testCheckIfAppHasAutoscalarAttached() throws Exception {
    deploymentManager.checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData.builder().build(), logCallback);
    verify(client, times(1)).checkIfAppHasAutoscalarAttached(any(), any());
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testIsActiveApplication() throws Exception {
    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().applicationName("app_1").build();
    Map<String, String> userProvider = new HashMap<>();
    userProvider.put(HARNESS__STATUS__IDENTIFIER, HARNESS__ACTIVE__IDENTIFIER);
    ApplicationEnvironments environments = ApplicationEnvironments.builder().userProvided(userProvider).build();

    when(client.getApplicationEnvironmentsByName(eq(pcfRequestConfig))).thenReturn(environments);
    assertThat(deploymentManager.isActiveApplication(pcfRequestConfig, logCallback)).isEqualTo(true);

    reset(client);
    when(client.getApplicationEnvironmentsByName(eq(pcfRequestConfig)))
        .thenReturn(ApplicationEnvironments.builder().build());
    assertThat(deploymentManager.isActiveApplication(pcfRequestConfig, logCallback)).isEqualTo(false);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testUpSizeApplicationWithSteadyStateCheckFail() throws Exception {
    StartedProcess startedProcess = mock(StartedProcess.class);
    Process process = mock(Process.class);

    doReturn(startedProcess).when(deploymentManager).startTailingLogsIfNeeded(any(), any(), any());
    doReturn(process).when(startedProcess).getProcess();
    doReturn(process).when(process).destroyForcibly();
    doNothing().when(process).destroy();

    PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder().desiredCount(1).timeOutIntervalInMins(1).build();
    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(2.0)
                                         .diskQuota((long) 2.23)
                                         .diskUsage((long) 1.23)
                                         .index("0")
                                         .memoryQuota((long) 2)
                                         .memoryUsage((long) 2)
                                         .state("RUNNING")
                                         .build();
    ApplicationDetail applicationDetail = generateApplicationDetail(1, new InstanceDetail[] {instanceDetail1});
    doReturn(applicationDetail).when(deploymentManager).resizeApplication(eq(pcfRequestConfig));
    doThrow(InterruptedException.class).when(client).getApplicationByName(eq(pcfRequestConfig));
    assertThatThrownBy(() -> deploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, logCallback))
        .isInstanceOf(PivotalClientApiException.class);

    reset(client);
    doReturn(applicationDetail).when(client).getApplicationByName(eq(pcfRequestConfig));
    doThrow(Exception.class).when(deploymentManager).destroyProcess(eq(startedProcess));
    ApplicationDetail applicationDetail1 =
        deploymentManager.upsizeApplicationWithSteadyStateCheck(pcfRequestConfig, logCallback);
    assertThat(applicationDetail1).isNotNull();
  }
}
