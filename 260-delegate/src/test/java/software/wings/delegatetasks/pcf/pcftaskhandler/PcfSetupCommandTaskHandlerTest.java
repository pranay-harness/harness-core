/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.IVAN;

import static software.wings.delegatetasks.pcf.PcfTestConstants.ACCOUNT_ID;
import static software.wings.delegatetasks.pcf.PcfTestConstants.MANIFEST_YAML;
import static software.wings.delegatetasks.pcf.PcfTestConstants.ORG;
import static software.wings.delegatetasks.pcf.PcfTestConstants.RUNNING;
import static software.wings.delegatetasks.pcf.PcfTestConstants.SPACE;
import static software.wings.delegatetasks.pcf.PcfTestConstants.STOPPED;
import static software.wings.delegatetasks.pcf.PcfTestConstants.getPcfConfig;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfSetupCommandResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfManifestFileData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.request.CfCommandSetupRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class PcfSetupCommandTaskHandlerTest extends WingsBaseTest {
  @Mock EncryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock LogCallback executionLogCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @Mock CfDeploymentManager pcfDeploymentManager;
  @Mock SecretDecryptionService secretDecryptionService;

  @InjectMocks @Spy PcfCommandTaskHelper pcfCommandTaskHelper;
  @InjectMocks @Spy PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper;
  @InjectMocks PcfSetupCommandTaskHandler pcfSetupCommandTaskHandler;

  @Test
  @Owner(developers = {ADWAIT, IVAN})
  @Category(UnitTests.class)
  public void testPerformSetup() throws PivotalClientApiException, IOException, ExecutionException {
    doReturn(executionLogCallback).when(logStreamingTaskClient).obtainLogCallback(anyString());
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder().metadataOnly(false).build();

    CfCommandRequest cfCommandRequest = CfCommandSetupRequest.builder()
                                            .pcfCommandType(CfCommandRequest.PcfCommandType.SETUP)
                                            .pcfConfig(getPcfConfig())
                                            .artifactFiles(Collections.emptyList())
                                            .artifactStreamAttributes(artifactStreamAttributes)
                                            .manifestYaml(MANIFEST_YAML)
                                            .organization(ORG)
                                            .space(SPACE)
                                            .accountId(ACCOUNT_ID)
                                            .routeMaps(Arrays.asList("ab.rc", "ab.ty/asd"))
                                            .timeoutIntervalInMin(5)
                                            .releaseNamePrefix("a_s_e")
                                            .olderActiveVersionCountToKeep(2)
                                            .maxCount(2)
                                            .build();

    // mocking
    List<ApplicationSummary> previousReleases = new ArrayList<>();
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__1")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__2")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__3")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(1)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__4")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(1)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__5")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(0)
                             .build());
    doReturn(previousReleases).when(pcfDeploymentManager).getPreviousReleases(any(), anyString());
    doReturn(previousReleases).when(pcfDeploymentManager).getDeployedServicesWithNonZeroInstances(any(), anyString());
    doNothing().when(pcfDeploymentManager).unmapRouteMapForApplication(any(), anyList(), any());
    doReturn("PASSWORD".toCharArray()).when(pcfCommandTaskHelper).getPassword(any());
    doReturn(CfInternalConfig.builder().build()).when(secretDecryptionService).decrypt(any(), any(), anyBoolean());
    doNothing().when(pcfDeploymentManager).deleteApplication(any());
    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .requestedState(STOPPED)
                 .stack("")
                 .runningInstances(0)
                 .urls(Arrays.asList("1.com", "2.com"))
                 .build())
        .when(pcfDeploymentManager)
        .resizeApplication(any());
    File f1 = new File("./test1");
    File f2 = new File("./test2");
    doReturn(f1).when(pcfCommandTaskHelper).downloadArtifactFromManager(any(), any(), any());
    doReturn(f2).when(pcfCommandTaskBaseHelper).createManifestYamlFileLocally(any());
    doNothing().when(pcfCommandTaskBaseHelper).deleteCreatedFile(anyList());

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__6")
                 .requestedState(STOPPED)
                 .stack("")
                 .runningInstances(0)
                 .build())
        .when(pcfDeploymentManager)
        .createApplication(any(), any());

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfSetupCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(pcfDeploymentManager, times(1)).createApplication(any(), any());
    verify(pcfDeploymentManager, times(3)).deleteApplication(any());
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    verify(pcfDeploymentManager, times(1)).unmapRouteMapForApplication(any(), anyList(), any());

    CfSetupCommandResponse cfSetupCommandResponse =
        (CfSetupCommandResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(CommandExecutionStatus.SUCCESS).isEqualTo(cfCommandExecutionResponse.getCommandExecutionStatus());
    assertThat(cfSetupCommandResponse.getNewApplicationDetails()).isNotNull();
    assertThat("a_s_e__6").isEqualTo(cfSetupCommandResponse.getNewApplicationDetails().getApplicationName());
    assertThat("10").isEqualTo(cfSetupCommandResponse.getNewApplicationDetails().getApplicationGuid());

    assertThat(cfSetupCommandResponse.getDownsizeDetails()).isNotNull();
    assertThat(cfSetupCommandResponse.getDownsizeDetails()).hasSize(1);
    Set<String> appsToBeDownsized = new HashSet<>(cfSetupCommandResponse.getDownsizeDetails()
                                                      .stream()
                                                      .map(CfAppSetupTimeDetails::getApplicationName)
                                                      .collect(toList()));
    assertThat(appsToBeDownsized.contains("a_s_e__4")).isTrue();
  }

  @Test
  @Owner(developers = {ADWAIT, IVAN})
  @Category(UnitTests.class)
  public void testCheckIfVarsFilePresent() {
    PcfManifestsPackage manifestsPackage = PcfManifestsPackage.builder().build();
    CfCommandSetupRequest setupRequest = CfCommandSetupRequest.builder().pcfManifestsPackage(manifestsPackage).build();
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();

    manifestsPackage.setVariableYmls(emptyList());
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();

    String str = null;
    manifestsPackage.setVariableYmls(Arrays.asList(str));
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrepareVarsYamlFile() throws Exception {
    File f1 = mock(File.class);
    File f2 = mock(File.class);
    doReturn(f1)
        .doReturn(f2)
        .when(pcfCommandTaskBaseHelper)
        .createManifestVarsYamlFileLocally(any(), anyString(), anyInt());

    CfCommandSetupRequest setupRequest =
        CfCommandSetupRequest.builder()
            .pcfManifestsPackage(PcfManifestsPackage.builder().variableYmls(Arrays.asList("a:b", "c:d")).build())
            .releaseNamePrefix("abc")
            .build();
    CfCreateApplicationRequestData requestData =
        CfCreateApplicationRequestData.builder()
            .password("ABCD".toCharArray())
            .varsYmlFilePresent(true)
            .pcfManifestFileData(CfManifestFileData.builder().varFiles(new ArrayList<>()).build())
            .build();

    pcfSetupCommandTaskHandler.prepareVarsYamlFile(requestData, setupRequest);

    assertThat(requestData.getPcfManifestFileData()).isNotNull();
    assertThat(requestData.getPcfManifestFileData().getVarFiles()).isNotEmpty();
    assertThat(requestData.getPcfManifestFileData().getVarFiles().size()).isEqualTo(2);
    assertThat(requestData.getPcfManifestFileData().getVarFiles()).containsExactly(f1, f2);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownsizeApplicationToZero() throws Exception {
    reset(pcfDeploymentManager);
    ApplicationSummary applicationSummary = ApplicationSummary.builder()
                                                .name("a_s_e__1")
                                                .diskQuota(1)
                                                .requestedState(RUNNING)
                                                .id("10")
                                                .instances(0)
                                                .memoryLimit(1)
                                                .runningInstances(0)
                                                .build();

    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name("a_s_e__1")
                                              .requestedState(STOPPED)
                                              .stack("")
                                              .runningInstances(0)
                                              .build();
    doReturn(applicationDetail).when(pcfDeploymentManager).resizeApplication(any());

    CfCommandSetupRequest cfCommandSetupRequest = CfCommandSetupRequest.builder().useAppAutoscalar(true).build();

    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData =
        CfAppAutoscalarRequestData.builder().configPathVar("path").build();

    doReturn(true).when(pcfCommandTaskBaseHelper).disableAutoscalar(any(), any());
    ArgumentCaptor<CfAppAutoscalarRequestData> argumentCaptor =
        ArgumentCaptor.forClass(CfAppAutoscalarRequestData.class);
    pcfSetupCommandTaskHandler.downsizeApplicationToZero(applicationSummary, CfRequestConfig.builder().build(),
        cfCommandSetupRequest, pcfAppAutoscalarRequestData, executionLogCallback);

    verify(pcfCommandTaskBaseHelper, times(1)).disableAutoscalar(argumentCaptor.capture(), any());
    pcfAppAutoscalarRequestData = argumentCaptor.getValue();
    assertThat(pcfAppAutoscalarRequestData.getApplicationGuid()).isEqualTo("10");
    assertThat(pcfAppAutoscalarRequestData.getApplicationName()).isEqualTo("a_s_e__1");
    assertThat(pcfAppAutoscalarRequestData.isExpectedEnabled()).isTrue();
  }
}
