package io.harness.delegate.cf;

import static io.harness.delegate.cf.PcfCommandTaskBaseHelper.DELIMITER;
import static io.harness.delegate.cf.PcfCommandTaskBaseHelperTest.ACTIVITY_ID;
import static io.harness.delegate.cf.PcfCommandTaskBaseHelperTest.APP_ID;
import static io.harness.delegate.task.pcf.request.CfInfraMappingDataRequest.ActionType.RUNNING_COUNT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.BOJANA;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
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

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.CfCommandRequest.PcfCommandType;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.request.CfCommandDeployRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;
import io.harness.delegate.task.pcf.request.CfInfraMappingDataRequest;
import io.harness.delegate.task.pcf.request.CfInfraMappingDataRequest.ActionType;
import io.harness.delegate.task.pcf.request.CfInstanceSyncRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfDeployCommandResponse;
import io.harness.delegate.task.pcf.response.CfInfraMappingDataResponse;
import io.harness.delegate.task.pcf.response.CfInstanceSyncResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDP)
public class PcfCommandTaskHandlerTest extends CategoryTest {
  public static final String URL = "URL";
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String RUNNING = "RUNNING";
  public static final String APP_NAME = "APP_NAME";
  public static final char[] USER_NAME_DECRYPTED = "USER_NAME_DECRYPTED".toCharArray();

  @Mock CfDeploymentManager pcfDeploymentManager;
  @Mock SecretDecryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock LogCallback executionLogCallback;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;
  @InjectMocks @Spy PcfCommandTaskBaseHelper pcfCommandTaskHelper;
  @InjectMocks @Inject PcfDeployCommandTaskHandler pcfDeployCommandTaskHandler;
  @InjectMocks @Spy PcfRouteUpdateCommandTaskHandler pcfRouteUpdateCommandTaskHandler;
  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;
  @InjectMocks @Inject PcfDataFetchCommandTaskHandler pcfDataFetchCommandTaskHandler;
  @InjectMocks @Inject PcfApplicationDetailsCommandTaskHandler pcfApplicationDetailsCommandTaskHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(executionLogCallback).when(logStreamingTaskClient).obtainLogCallback(anyString());
  }

  private CfInternalConfig getPcfConfig() {
    return CfInternalConfig.builder().username(USER_NAME_DECRYPTED).endpointUrl(URL).password(new char[0]).build();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformDeploy_nonBlueGreen() throws Exception {
    CfCommandRequest cfCommandRequest =
        CfCommandDeployRequest.builder()
            .pcfCommandType(PcfCommandType.RESIZE)
            .resizeStrategy(ResizeStrategy.DOWNSIZE_OLD_FIRST)
            .pcfConfig(getPcfConfig())
            .accountId(ACCOUNT_ID)
            .newReleaseName("a_s_e__6")
            .organization(ORG)
            .space(SPACE)
            .updateCount(2)
            .downSizeCount(1)
            .totalPreviousInstanceCount(2)
            .timeoutIntervalInMin(2)
            .downsizeAppDetail(
                CfAppSetupTimeDetails.builder().applicationName("a_s_e__4").initialInstanceCount(2).build())
            .build();

    ApplicationDetail applicationDetailNew = ApplicationDetail.builder()
                                                 .id("10")
                                                 .diskQuota(1)
                                                 .instances(0)
                                                 .memoryLimit(1)
                                                 .name("a_s_e__6")
                                                 .requestedState("STOPPED")
                                                 .stack("")
                                                 .runningInstances(0)
                                                 .build();

    ApplicationDetail applicationDetailOld = ApplicationDetail.builder()
                                                 .id("10")
                                                 .diskQuota(1)
                                                 .instances(2)
                                                 .memoryLimit(1)
                                                 .name("a_s_e__4")
                                                 .requestedState("RUNNING")
                                                 .stack("")
                                                 .runningInstances(0)
                                                 .build();
    doReturn(applicationDetailNew)
        .doReturn(applicationDetailOld)
        .doReturn(applicationDetailNew)
        .when(pcfDeploymentManager)
        .getApplicationByName(any());

    List<ApplicationSummary> previousReleases = new ArrayList<>();
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__6")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(1)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__4")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(2)
                             .memoryLimit(1)
                             .runningInstances(1)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__3")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(0)
                             .memoryLimit(1)
                             .runningInstances(1)
                             .build());

    doReturn(previousReleases).when(pcfDeploymentManager).getPreviousReleases(any(), anyString());
    doReturn(previousReleases).when(pcfDeploymentManager).getDeployedServicesWithNonZeroInstances(any(), anyString());

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(1)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(0)
                 .instanceDetails(InstanceDetail.builder()
                                      .cpu(1.0)
                                      .diskQuota((long) 1.23)
                                      .diskUsage((long) 1.23)
                                      .index("0")
                                      .memoryQuota((long) 1)
                                      .memoryUsage((long) 1)
                                      .build())
                 .build())
        .when(pcfDeploymentManager)
        .resizeApplication(any());

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .requestedState("RUNNING")
                 .stack("")
                 .diskQuota(1)
                 .instances(2)
                 .memoryLimit(1)
                 .name("a_s_e__6")
                 .runningInstances(2)
                 .instanceDetails(InstanceDetail.builder()
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
                         .build())
                 .build())
        .when(pcfDeploymentManager)
        .upsizeApplicationWithSteadyStateCheck(any(), any());

    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfDeployCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CfDeployCommandResponse pcfDeployCommandResponse =
        (CfDeployCommandResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfDeployCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    List<CfServiceData> cfServiceData = pcfDeployCommandResponse.getInstanceDataUpdated();
    assertThat(cfServiceData).hasSize(2);
    for (CfServiceData data : cfServiceData) {
      if (data.getName().equals("a_s_e__4")) {
        assertThat(data.getPreviousCount()).isEqualTo(2);
        assertThat(data.getDesiredCount()).isEqualTo(1);
      } else if (data.getName().equals("a_s_e__6")) {
        assertThat(data.getPreviousCount()).isEqualTo(0);
        assertThat(data.getDesiredCount()).isEqualTo(2);
      }
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConfigureAutoscalarIfNeeded() throws Exception {
    CfCommandDeployRequest pcfCommandRequest = CfCommandDeployRequest.builder()
                                                   .downSizeCount(1)
                                                   .totalPreviousInstanceCount(2)
                                                   .timeoutIntervalInMin(2)
                                                   .useAppAutoscalar(false)
                                                   .maxCount(1)
                                                   .updateCount(1)
                                                   .build();

    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id(APP_ID)
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name(APP_NAME)
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(0)
                                              .build();

    reset(pcfDeploymentManager);
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());

    // Autoscalar is false
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandRequest, applicationDetail, null, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // Autoscalar is true, but no autosaclar file
    pcfCommandRequest.setUseAppAutoscalar(true);
    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandRequest, applicationDetail, null, executionLogCallback);
    verify(pcfDeploymentManager, never()).performConfigureAutoscalar(any(), any());

    // Autoscalar is true, autoscalar file is present
    String path = "./test" + System.currentTimeMillis();
    FileIo.createDirectoryIfDoesNotExist(path);

    pcfCommandRequest.setPcfManifestsPackage(PcfManifestsPackage.builder().autoscalarManifestYml("abc").build());
    CfAppAutoscalarRequestData autoscalarRequestData = CfAppAutoscalarRequestData.builder()
                                                           .applicationName(APP_NAME)
                                                           .applicationGuid(APP_ID)
                                                           .configPathVar(path)
                                                           .build();

    pcfDeployCommandTaskHandler.configureAutoscalarIfNeeded(
        pcfCommandRequest, applicationDetail, autoscalarRequestData, executionLogCallback);
    ArgumentCaptor<CfAppAutoscalarRequestData> captor = ArgumentCaptor.forClass(CfAppAutoscalarRequestData.class);
    verify(pcfDeploymentManager, times(1)).performConfigureAutoscalar(captor.capture(), any());
    autoscalarRequestData = captor.getValue();
    assertThat(autoscalarRequestData.getApplicationName()).isEqualTo(APP_NAME);
    assertThat(autoscalarRequestData.getApplicationGuid()).isEqualTo(APP_ID);
    String filePath = autoscalarRequestData.getAutoscalarFilePath();

    String content = new String(Files.readAllBytes(Paths.get(filePath)));
    assertThat(content).isEqualTo("abc");
    FileIo.deleteDirectoryAndItsContentIfExists(path);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformRollback() throws Exception {
    CfCommandRequest cfCommandRequest =
        CfCommandRollbackRequest.builder()
            .pcfCommandType(PcfCommandType.ROLLBACK)
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
                CfAppSetupTimeDetails.builder().applicationName("a_s_e__6").urls(Collections.EMPTY_LIST).build())
            .build();

    doReturn(ApplicationDetail.builder()
                 .id("Guid:a_s_e__6")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(0)
                 .build())
        .doReturn(ApplicationDetail.builder()
                      .id("Guid:a_s_e__4")
                      .diskQuota(1)
                      .instances(1)
                      .memoryLimit(1)
                      .name("a_s_e__4")
                      .requestedState("STOPPED")
                      .stack("")
                      .runningInstances(0)
                      .build())
        .when(pcfDeploymentManager)
        .getApplicationByName(any());

    ApplicationDetail applicationDetailDownsize = ApplicationDetail.builder()
                                                      .id("Guid:a_s_e__6")
                                                      .diskQuota(1)
                                                      .instances(0)
                                                      .memoryLimit(1)
                                                      .name("a_s_e__6")
                                                      .requestedState("STOPPED")
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
        .when(pcfDeploymentManager)
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
        .when(pcfDeploymentManager)
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
  public void testEnableAutoscalarIfNeeded() throws Exception {
    reset(pcfDeploymentManager);

    CfServiceData cfServiceData = CfServiceData.builder().name(APP_NAME).id(APP_ID).build();
    List<CfServiceData> upsizeList = Arrays.asList(cfServiceData);

    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData =
        CfAppAutoscalarRequestData.builder().configPathVar("path").build();
    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());

    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        emptyList(), pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).changeAutoscalarState(any(), any(), anyBoolean());

    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        upsizeList, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, never()).changeAutoscalarState(any(), any(), anyBoolean());

    cfServiceData.setDisableAutoscalarPerformed(true);
    pcfRollbackCommandTaskHandler.enableAutoscalarIfNeeded(
        upsizeList, pcfAppAutoscalarRequestData, executionLogCallback);
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testperformDataFetch() throws Exception {
    CfInfraMappingDataRequest pcfCommandRequest = CfInfraMappingDataRequest.builder()
                                                      .pcfCommandType(PcfCommandType.DATAFETCH)
                                                      .pcfConfig(getPcfConfig())
                                                      .accountId(ACCOUNT_ID)
                                                      .timeoutIntervalInMin(5)
                                                      .actionType(ActionType.FETCH_ORG)
                                                      .build();

    doReturn(Arrays.asList(ORG)).when(pcfDeploymentManager).getOrganizations(any());
    doReturn(Arrays.asList(SPACE)).when(pcfDeploymentManager).getSpacesForOrganization(any());
    doReturn(Arrays.asList("R1", "R2")).when(pcfDeploymentManager).getRouteMaps(any());

    // Fetch Orgs
    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CfInfraMappingDataResponse pcfInfraMappingDataResponse =
        (CfInfraMappingDataResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInfraMappingDataResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfInfraMappingDataResponse.getOrganizations()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getOrganizations()).hasSize(1);
    assertThat(pcfInfraMappingDataResponse.getOrganizations().get(0)).isEqualTo(ORG);

    // Fetch Spaces for org
    pcfCommandRequest.setActionType(ActionType.FETCH_SPACE);
    pcfCommandRequest.setOrganization(ORG);
    cfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    pcfInfraMappingDataResponse = (CfInfraMappingDataResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInfraMappingDataResponse.getSpaces()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getSpaces()).hasSize(1);
    assertThat(pcfInfraMappingDataResponse.getSpaces().get(0)).isEqualTo(SPACE);

    // Fetch Routes
    pcfCommandRequest.setActionType(ActionType.FETCH_ROUTE);
    pcfCommandRequest.setSpace(SPACE);
    cfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, logStreamingTaskClient, false);
    pcfInfraMappingDataResponse = (CfInfraMappingDataResponse) cfCommandExecutionResponse.getPcfCommandResponse();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps()).hasSize(2);
    assertThat(pcfInfraMappingDataResponse.getRouteMaps().contains("R1")).isTrue();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps().contains("R2")).isTrue();

    // Fetch running count
    String appNamePrefix = "App";
    ApplicationSummary applicationSummary = ApplicationSummary.builder()
                                                .id("id1")
                                                .name(appNamePrefix + DELIMITER + "1")
                                                .diskQuota(1)
                                                .instances(1)
                                                .memoryLimit(1)
                                                .requestedState("RUNNING")
                                                .runningInstances(2)
                                                .build();
    doReturn(Collections.singletonList(applicationSummary))
        .when(pcfDeploymentManager)
        .getPreviousReleases(any(CfRequestConfig.class), eq(appNamePrefix));
    pcfCommandRequest.setApplicationNamePrefix(appNamePrefix);
    pcfCommandRequest.setActionType(RUNNING_COUNT);
    cfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, logStreamingTaskClient, false);
    pcfInfraMappingDataResponse = (CfInfraMappingDataResponse) cfCommandExecutionResponse.getPcfCommandResponse();
    assertThat(pcfInfraMappingDataResponse).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getRunningInstanceCount()).isEqualTo(2);

    // Fetch running count failure
    doThrow(Exception.class)
        .when(pcfDeploymentManager)
        .getPreviousReleases(any(CfRequestConfig.class), eq(appNamePrefix));
    cfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, logStreamingTaskClient, false);
    assertThat(cfCommandExecutionResponse).isNotNull();
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);

    CfCommandDeployRequest deployRequest = CfCommandDeployRequest.builder().build();
    assertThatThrownBy(
        () -> pcfDataFetchCommandTaskHandler.executeTaskInternal(deployRequest, null, logStreamingTaskClient, false))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testperformAppDetails() throws Exception {
    CfInstanceSyncRequest cfInstanceSyncRequest = CfInstanceSyncRequest.builder()
                                                      .pcfCommandType(PcfCommandType.APP_DETAILS)
                                                      .pcfApplicationName("APP_NAME")
                                                      .pcfConfig(getPcfConfig())
                                                      .accountId(ACCOUNT_ID)
                                                      .timeoutIntervalInMin(5)
                                                      .build();

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(1)
                 .memoryLimit(1)
                 .name("a_s_e__6")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(1)
                 .instanceDetails(Arrays.asList(InstanceDetail.builder()
                                                    .cpu(1.0)
                                                    .diskQuota((long) 1.23)
                                                    .diskUsage((long) 1.23)
                                                    .index("2")
                                                    .memoryQuota((long) 1)
                                                    .memoryUsage((long) 1)
                                                    .build()))
                 .id("Guid:a_s_e__3")
                 .diskQuota(1)
                 .instances(1)
                 .memoryLimit(1)
                 .name("a_s_e__3")
                 .requestedState("RUNNING")
                 .stack("")
                 .runningInstances(1)
                 .build())
        .when(pcfDeploymentManager)
        .getApplicationByName(any());

    // Fetch Orgs
    CfCommandExecutionResponse cfCommandExecutionResponse = pcfApplicationDetailsCommandTaskHandler.executeTaskInternal(
        cfInstanceSyncRequest, null, logStreamingTaskClient, false);

    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CfInstanceSyncResponse pcfInstanceSyncResponse =
        (CfInstanceSyncResponse) cfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInstanceSyncResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfInstanceSyncResponse.getInstanceIndices()).isNotNull();
    assertThat(pcfInstanceSyncResponse.getInstanceIndices()).hasSize(1);
    assertThat(pcfInstanceSyncResponse.getInstanceIndices().get(0)).isEqualTo("2");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPerformSwapRouteExecute() throws Exception {
    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        CfRouteUpdateRequestConfigData.builder()
            .downsizeOldApplication(false)
            .finalRoutes(Arrays.asList("a.b.c"))
            .isRollback(true)
            .isStandardBlueGreen(true)
            .existingApplicationDetails(
                Arrays.asList(CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()))
            .build();
    CfCommandRequest cfCommandRequest = getRouteUpdateRequest(routeUpdateRequestConfigData);

    doNothing().when(pcfCommandTaskHelper).mapRouteMaps(anyString(), anyList(), any(), any());
    doNothing().when(pcfCommandTaskHelper).unmapRouteMaps(anyString(), anyList(), any(), any());
    doReturn(null).when(pcfDeploymentManager).upsizeApplicationWithSteadyStateCheck(any(), any());
    doReturn(null).when(pcfDeploymentManager).resizeApplication(any());

    // 2 Rollback True, existingApplication : available
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    CfCommandExecutionResponse cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(pcfDeploymentManager, times(1)).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 3 Rollback True, existingApplication : unavailable
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(pcfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 4 Rollback False, existingApplication : unavailable
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setRollback(false);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(pcfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 5  Rollback False, existingApplication : available
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(
        Arrays.asList(CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()));
    cfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(cfCommandRequest, null, logStreamingTaskClient, false);
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    verify(pcfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());
    assertThat(cfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testResizeOldApplications() throws Exception {
    List<CfAppSetupTimeDetails> appSetupTimeDetailsList =
        Arrays.asList(CfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build());
    CfRouteUpdateRequestConfigData routeUpdateRequestConfigData = CfRouteUpdateRequestConfigData.builder()
                                                                      .downsizeOldApplication(false)
                                                                      .finalRoutes(Arrays.asList("a.b.c"))
                                                                      .isRollback(false)
                                                                      .isStandardBlueGreen(true)
                                                                      .build();

    CfCommandRouteUpdateRequest pcfCommandRequest = CfCommandRouteUpdateRequest.builder()
                                                        .pcfCommandType(PcfCommandType.RESIZE)
                                                        .pcfConfig(getPcfConfig())
                                                        .accountId(ACCOUNT_ID)
                                                        .organization(ORG)
                                                        .space(SPACE)
                                                        .timeoutIntervalInMin(2)
                                                        .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
                                                        .pcfRouteUpdateConfigData(routeUpdateRequestConfigData)
                                                        .useAppAutoscalar(false)
                                                        .build();

    reset(pcfDeploymentManager);

    // Existing applications are empty. No op expected
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(pcfDeploymentManager, never()).resizeApplication(any());

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
                                              .instanceDetails(Arrays.asList(InstanceDetail.builder()
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
                                              .requestedState("RUNNING")
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());
    doReturn(applicationDetail).when(pcfDeploymentManager).resizeApplication(any());

    doReturn(true).when(pcfCommandTaskHelper).disableAutoscalar(any(), any());
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    verify(pcfCommandTaskHelper, never()).disableAutoscalar(any(), any());

    // Autoscalar is true, existing app present
    pcfCommandRequest.setUseAppAutoscalar(true);
    ArgumentCaptor<CfAppAutoscalarRequestData> argumentCaptor =
        ArgumentCaptor.forClass(CfAppAutoscalarRequestData.class);
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, false, "");
    verify(pcfDeploymentManager, times(2)).resizeApplication(any());
    verify(pcfCommandTaskHelper, times(1)).disableAutoscalar(argumentCaptor.capture(), any());
    CfAppAutoscalarRequestData pcfAppAutoscalarRequestData = argumentCaptor.getValue();
    assertThat(pcfAppAutoscalarRequestData.getApplicationName()).isEqualTo("app1");
    assertThat(pcfAppAutoscalarRequestData.getApplicationGuid()).isEqualTo("10");
    assertThat(pcfAppAutoscalarRequestData.getTimeoutInMins()).isEqualTo(2);
    assertThat(pcfAppAutoscalarRequestData.isExpectedEnabled()).isTrue();

    routeUpdateRequestConfigData.setRollback(true);
    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfRouteUpdateCommandTaskHandler.resizeOldApplications(
        pcfCommandRequest, CfRequestConfig.builder().build(), executionLogCallback, true, "");
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(argumentCaptor.capture(), any(), anyBoolean());
    pcfAppAutoscalarRequestData = argumentCaptor.getValue();
    assertThat(pcfAppAutoscalarRequestData.getApplicationName()).isEqualTo("app1");
    assertThat(pcfAppAutoscalarRequestData.getApplicationGuid()).isEqualTo("10");
    assertThat(pcfAppAutoscalarRequestData.getTimeoutInMins()).isEqualTo(2);
    assertThat(pcfAppAutoscalarRequestData.isExpectedEnabled()).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSwapRouteExecutionNeeded() throws Exception {
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGeneratePcfInstancesElementsForExistingApp() throws Exception {
    List<CfInternalInstanceElement> pcfInstanceElements = new ArrayList<>();

    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(1)
                                              .memoryLimit(1)
                                              .name("app1")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(1)
                                              .instanceDetails(Arrays.asList(InstanceDetail.builder()
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
                                              .requestedState("RUNNING")
                                              .stack("")
                                              .runningInstances(1)
                                              .build();
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());

    CfCommandDeployRequest request = CfCommandDeployRequest.builder().build();
    pcfDeployCommandTaskHandler.generatePcfInstancesElementsForExistingApp(
        pcfInstanceElements, CfRequestConfig.builder().build(), request, executionLogCallback);

    request.setDownsizeAppDetail(CfAppSetupTimeDetails.builder().applicationName("app").build());
    pcfDeployCommandTaskHandler.generatePcfInstancesElementsForExistingApp(
        pcfInstanceElements, CfRequestConfig.builder().build(), request, executionLogCallback);
    assertThat(pcfInstanceElements.size()).isEqualTo(1);
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("2");

    doThrow(new PivotalClientApiException("e")).when(pcfDeploymentManager).getApplicationByName(any());
    pcfDeployCommandTaskHandler.generatePcfInstancesElementsForExistingApp(
        pcfInstanceElements, CfRequestConfig.builder().build(), request, executionLogCallback);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testpcfApplicationDetailsCommandTaskHandlerInvalidArgumentsException() throws IOException {
    CfCommandRequest cfCommandRequest = mock(CfCommandDeployRequest.class);
    when(cfCommandRequest.getActivityId()).thenReturn(ACTIVITY_ID);
    List<EncryptedDataDetail> encryptedDataDetails = Arrays.asList(EncryptedDataDetail.builder().build());
    when(encryptionService.getDecryptedValue(any())).thenReturn("decryptedValue".toCharArray());
    try {
      pcfApplicationDetailsCommandTaskHandler.executeTask(
          cfCommandRequest, encryptedDataDetails, false, logStreamingTaskClient);
    } catch (Exception e) {
      assertThatExceptionOfType(InvalidArgumentsException.class);
      InvalidArgumentsException invalidArgumentsException = (InvalidArgumentsException) e;
      assertThat(invalidArgumentsException.getParams())
          .containsValue("cfCommandRequest: Must be instance of CfInstanceSyncRequest");
    }
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveApp() throws Exception {
    reset(pcfDeploymentManager);
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
    doReturn(existingApplicationSummaries).when(pcfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

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
    verify(pcfDeploymentManager).upsizeApplicationWithSteadyStateCheck(any(), any());

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
    verify(pcfDeploymentManager, times(2))
        .setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
    List<Boolean> isActiveAppValues = isActiveAppCaptor.getAllValues();
    assertThat(isActiveAppValues).isNotNull();
    assertThat(isActiveAppValues.get(0)).isTrue(); // for active app
    assertThat(isActiveAppValues.get(1)).isFalse(); // for inactive app

    // verify un-setting of ENV variables for New app
    ArgumentCaptor<CfRequestConfig> cfRequestConfigArgumentCaptor = ArgumentCaptor.forClass(CfRequestConfig.class);
    verify(pcfDeploymentManager).unsetEnvironmentVariableForAppStatus(cfRequestConfigArgumentCaptor.capture(), any());
    CfRequestConfig cfRequestConfig = cfRequestConfigArgumentCaptor.getValue();
    assertThat(cfRequestConfig).isNotNull();
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(newAppName);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveAppNotPresent() throws Exception {
    reset(pcfDeploymentManager);
    String appPrefix = "cf_app";
    String inActiveAppName = appPrefix + "_4";
    String activeAppName = appPrefix + "_5";
    String newAppName = appPrefix + "_6";
    List<String> finalRoutes = Arrays.asList("basicRoute.apps.pcf-harness.com", "shiny-jackal-pa.apps.pcf-harness.com");
    List<String> tempRoutes = Collections.singletonList("tempBg.apps.pcf-harness.com");

    List<ApplicationSummary> existingApplicationSummaries =
        getExistingApplicationSummaries(appPrefix, inActiveAppName, activeAppName, newAppName);
    doReturn(existingApplicationSummaries).when(pcfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

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
    verify(pcfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());

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
    verify(pcfDeploymentManager).setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
    List<Boolean> isActiveAppValues = isActiveAppCaptor.getAllValues();
    assertThat(isActiveAppValues).isNotNull();
    assertThat(isActiveAppValues.size()).isEqualTo(1);
    assertThat(isActiveAppValues.get(0)).isTrue(); // for active app

    // verify un-setting of ENV variables for New app
    ArgumentCaptor<CfRequestConfig> cfRequestConfigArgumentCaptor = ArgumentCaptor.forClass(CfRequestConfig.class);
    verify(pcfDeploymentManager).unsetEnvironmentVariableForAppStatus(cfRequestConfigArgumentCaptor.capture(), any());
    CfRequestConfig cfRequestConfig = cfRequestConfigArgumentCaptor.getValue();
    assertThat(cfRequestConfig).isNotNull();
    assertThat(cfRequestConfig.getApplicationName()).isEqualTo(newAppName);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapRouteRollbackForInActiveAppFeatureFlagDisabled() throws Exception {
    reset(pcfDeploymentManager);
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
    doReturn(existingApplicationSummaries).when(pcfDeploymentManager).getPreviousReleases(any(), eq(appPrefix));

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
    verify(pcfDeploymentManager, never()).upsizeApplicationWithSteadyStateCheck(any(), any());

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
    verify(pcfDeploymentManager, times(2))
        .setEnvironmentVariableForAppStatus(any(), isActiveAppCaptor.capture(), any());
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
            .requestedState("RUNNING")
            .runningInstances(2)
            .build(),
        ApplicationSummary.builder()
            .id("806c5057-10d4-44c1-ba1b-9e56bd5a997f")
            .name(activeAppName)
            .diskQuota(1)
            .instances(2)
            .memoryLimit(250)
            .requestedState("RUNNING")
            .runningInstances(2)
            .build(),
        ApplicationSummary.builder()
            .id("914d10c2-76e4-4467-96c7-688b0be7e8ad")
            .name(newAppName)
            .diskQuota(1)
            .instances(2)
            .memoryLimit(250)
            .requestedState("RUNNING")
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

  private CfCommandRouteUpdateRequest getRouteUpdateRequest(
      CfRouteUpdateRequestConfigData routeUpdateRequestConfigData) {
    return CfCommandRouteUpdateRequest.builder()
        .pcfCommandType(PcfCommandType.RESIZE)
        .pcfConfig(getPcfConfig())
        .accountId(ACCOUNT_ID)
        .organization(ORG)
        .space(SPACE)
        .timeoutIntervalInMin(2)
        .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
        .pcfRouteUpdateConfigData(routeUpdateRequestConfigData)
        .build();
  }
}
