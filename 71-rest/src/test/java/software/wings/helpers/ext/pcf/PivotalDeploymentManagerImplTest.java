package software.wings.helpers.ext.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
  }

  @Test
  @Owner(developers = ADWAIT)
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
}
