package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.pcf.PcfManifestFileData;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.security.encryption.EncryptedDataDetail;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest.ActionType;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PcfCommandTaskHandlerTest extends WingsBaseTest {
  public static final String USERNMAE = "USERNMAE";
  public static final String URL = "URL";
  public static final String MANIFEST_YAML = "  applications:\n"
      + "  - name : ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances : ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      - route: ${ROUTE_MAP}\n";

  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String RUNNING = "RUNNING";

  @Mock PcfDeploymentManager pcfDeploymentManager;
  @Mock EncryptionService encryptionService;
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock ExecutionLogCallback executionLogCallback;
  @Mock DelegateFileManager delegateFileManager;
  @InjectMocks @Spy PcfCommandTaskHelper pcfCommandTaskHelper;
  @InjectMocks @Inject PcfSetupCommandTaskHandler pcfSetupCommandTaskHandler;
  @InjectMocks @Inject PcfDeployCommandTaskHandler pcfDeployCommandTaskHandler;
  @InjectMocks @Spy PcfRouteUpdateCommandTaskHandler pcfRouteUpdateCommandTaskHandler;
  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;
  @InjectMocks @Inject PcfDataFetchCommandTaskHandler pcfDataFetchCommandTaskHandler;
  @InjectMocks @Inject PcfApplicationDetailsCommandTaskHandler pcfApplicationDetailsCommandTaskHandler;

  @Test
  @Category(UnitTests.class)
  public void testPerformSetup() throws Exception {
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    PcfCommandRequest pcfCommandRequest = PcfCommandSetupRequest.builder()
                                              .pcfCommandType(PcfCommandType.SETUP)
                                              .pcfConfig(getPcfConfig())
                                              .artifactFiles(Collections.EMPTY_LIST)
                                              .manifestYaml(MANIFEST_YAML)
                                              .organization(ORG)
                                              .space(SPACE)
                                              .accountId(ACCOUNT_ID)
                                              .routeMaps(Arrays.asList("ab.rc", "ab.ty/asd"))
                                              .timeoutIntervalInMin(5)
                                              .releaseNamePrefix("a_s_e")
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
    doNothing().when(pcfDeploymentManager).unmapRouteMapForApplication(any(), anyList());

    doNothing().when(pcfDeploymentManager).deleteApplication(any());
    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(0)
                 .urls(Arrays.asList("1.com", "2.com"))
                 .build())
        .when(pcfDeploymentManager)
        .resizeApplication(any());
    File f1 = new File("./test1");
    File f2 = new File("./test2");
    doReturn(f1).when(pcfCommandTaskHelper).downloadArtifact(any(), any(), any());
    doReturn(f2).when(pcfCommandTaskHelper).createManifestYamlFileLocally(any());
    doNothing().when(pcfCommandTaskHelper).deleteCreatedFile(anyList());

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__6")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(0)
                 .build())
        .when(pcfDeploymentManager)
        .createApplication(any(), any());

    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfSetupCommandTaskHandler.executeTask(pcfCommandRequest, null);
    verify(pcfDeploymentManager, times(1)).createApplication(any(), any());
    verify(pcfDeploymentManager, times(3)).deleteApplication(any());
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    verify(pcfDeploymentManager, times(1)).unmapRouteMapForApplication(any(), anyList());

    PcfSetupCommandResponse pcfSetupCommandResponse =
        (PcfSetupCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(CommandExecutionStatus.SUCCESS).isEqualTo(pcfCommandExecutionResponse.getCommandExecutionStatus());
    assertThat(pcfSetupCommandResponse.getNewApplicationDetails()).isNotNull();
    assertThat("a_s_e__6").isEqualTo(pcfSetupCommandResponse.getNewApplicationDetails().getApplicationName());
    assertThat("10").isEqualTo(pcfSetupCommandResponse.getNewApplicationDetails().getApplicationGuid());

    assertThat(pcfSetupCommandResponse.getDownsizeDetails()).isNotNull();
    assertThat(pcfSetupCommandResponse.getDownsizeDetails()).hasSize(2);
    Set<String> appsToBeDownsized = new HashSet<>(
        pcfSetupCommandResponse.getDownsizeDetails().stream().map(app -> app.getApplicationName()).collect(toList()));
    assertThat(appsToBeDownsized.contains("a_s_e__3")).isTrue();
    assertThat(appsToBeDownsized.contains("a_s_e__4")).isTrue();
  }

  private PcfConfig getPcfConfig() {
    return PcfConfig.builder().username(USERNMAE).endpointUrl(URL).password(new char[0]).build();
  }

  @Test
  @Category(UnitTests.class)
  public void testPerformDeploy_nonBlueGreen() throws Exception {
    PcfCommandRequest pcfCommandRequest =
        PcfCommandDeployRequest.builder()
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
                PcfAppSetupTimeDetails.builder().applicationName("a_s_e__4").initialInstanceCount(2).build())
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
        .doReturn(ApplicationDetail.builder()
                      .id("10")
                      .diskQuota(1)
                      .instances(2)
                      .memoryLimit(1)
                      .name("a_s_e__6")
                      .requestedState("RUNNING")
                      .stack("")
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
        .resizeApplication(any());

    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfDeployCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);

    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfDeployCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    List<PcfServiceData> pcfServiceDatas = pcfDeployCommandResponse.getInstanceDataUpdated();
    assertThat(pcfServiceDatas).hasSize(2);
    for (PcfServiceData data : pcfServiceDatas) {
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
  @Category(UnitTests.class)
  public void testPerformRollback() throws Exception {
    PcfCommandRequest pcfCommandRequest =
        PcfCommandRollbackRequest.builder()
            .pcfCommandType(PcfCommandType.ROLLBACK)
            .pcfConfig(getPcfConfig())
            .accountId(ACCOUNT_ID)
            .instanceData(
                Arrays.asList(PcfServiceData.builder().name("a_s_e__6").previousCount(2).desiredCount(0).build(),
                    PcfServiceData.builder().name("a_s_e__4").previousCount(0).desiredCount(2).build()))
            .resizeStrategy(ResizeStrategy.DOWNSIZE_OLD_FIRST)
            .organization(ORG)
            .space(SPACE)
            .timeoutIntervalInMin(5)
            .newApplicationDetails(
                PcfAppSetupTimeDetails.builder().applicationName("a_s_e__6").urls(Collections.EMPTY_LIST).build())
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
        .resizeApplication(any());

    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfRollbackCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);

    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfDeployCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfDeployCommandResponse.getPcfInstanceElements()).isNotNull();
    assertThat(pcfDeployCommandResponse.getPcfInstanceElements()).hasSize(2);

    Set<String> pcfInstanceElements = new HashSet<>();
    ((PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse())
        .getPcfInstanceElements()
        .forEach(pcfInstanceElement
            -> pcfInstanceElements.add(
                pcfInstanceElement.getApplicationId() + ":" + pcfInstanceElement.getInstanceIndex()));
    assertThat(pcfInstanceElements.contains("Guid:a_s_e__4:0")).isTrue();
    assertThat(pcfInstanceElements.contains("Guid:a_s_e__4:1")).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testperformDataFetch() throws Exception {
    PcfInfraMappingDataRequest pcfCommandRequest = PcfInfraMappingDataRequest.builder()
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
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);

    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    PcfInfraMappingDataResponse pcfInfraMappingDataResponse =
        (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInfraMappingDataResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfInfraMappingDataResponse.getOrganizations()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getOrganizations()).hasSize(1);
    assertThat(pcfInfraMappingDataResponse.getOrganizations().get(0)).isEqualTo(ORG);

    // Fetch Spaces for org
    pcfCommandRequest.setActionType(ActionType.FETCH_SPACE);
    pcfCommandRequest.setOrganization(ORG);
    pcfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);

    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    pcfInfraMappingDataResponse = (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInfraMappingDataResponse.getSpaces()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getSpaces()).hasSize(1);
    assertThat(pcfInfraMappingDataResponse.getSpaces().get(0)).isEqualTo(SPACE);

    // Fetch Routes
    pcfCommandRequest.setActionType(ActionType.FETCH_ROUTE);
    pcfCommandRequest.setSpace(SPACE);
    pcfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);
    pcfInfraMappingDataResponse = (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps()).isNotNull();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps()).hasSize(2);
    assertThat(pcfInfraMappingDataResponse.getRouteMaps().contains("R1")).isTrue();
    assertThat(pcfInfraMappingDataResponse.getRouteMaps().contains("R2")).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testperformAppDetails() throws Exception {
    PcfInstanceSyncRequest pcfInstanceSyncRequest = PcfInstanceSyncRequest.builder()
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
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfApplicationDetailsCommandTaskHandler.executeTaskInternal(pcfInstanceSyncRequest, null, executionLogCallback);

    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        (PcfInstanceSyncResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertThat(pcfInstanceSyncResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(pcfInstanceSyncResponse.getInstanceIndices()).isNotNull();
    assertThat(pcfInstanceSyncResponse.getInstanceIndices()).hasSize(1);
    assertThat(pcfInstanceSyncResponse.getInstanceIndices().get(0)).isEqualTo("2");
  }

  @Test
  @Category(UnitTests.class)
  public void testPerformSwapRouteExecute() throws Exception {
    PcfRouteUpdateRequestConfigData routeUpdateRequestConfigData =
        PcfRouteUpdateRequestConfigData.builder()
            .downsizeOldApplication(false)
            .finalRoutes(Arrays.asList("a.b.c"))
            .isRollback(true)
            .isStandardBlueGreen(true)
            .existingApplicationDetails(
                Arrays.asList(PcfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()))
            .build();
    PcfCommandRequest pcfCommandRequest = PcfCommandRouteUpdateRequest.builder()
                                              .pcfCommandType(PcfCommandType.RESIZE)
                                              .pcfConfig(getPcfConfig())
                                              .accountId(ACCOUNT_ID)
                                              .organization(ORG)
                                              .space(SPACE)
                                              .timeoutIntervalInMin(2)
                                              .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
                                              .pcfRouteUpdateConfigData(routeUpdateRequestConfigData)
                                              .build();

    doNothing().when(pcfCommandTaskHelper).mapRouteMaps(anyString(), anyList(), any(), any());
    doNothing().when(pcfCommandTaskHelper).unmapRouteMaps(anyString(), anyList(), any(), any());

    // 2
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 3
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    pcfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);
    verify(pcfDeploymentManager, never()).resizeApplication(any());
    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 4
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setRollback(false);
    routeUpdateRequestConfigData.setExistingApplicationDetails(null);
    pcfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);
    verify(pcfDeploymentManager, times(0)).resizeApplication(any());
    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    // 5
    reset(pcfDeploymentManager);
    routeUpdateRequestConfigData.setDownsizeOldApplication(true);
    routeUpdateRequestConfigData.setExistingApplicationDetails(
        Arrays.asList(PcfAppSetupTimeDetails.builder().applicationName("app1").initialInstanceCount(1).build()));
    pcfCommandExecutionResponse =
        pcfRouteUpdateCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null, executionLogCallback);
    verify(pcfDeploymentManager, times(1)).resizeApplication(any());
    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Category(UnitTests.class)
  public void testCheckIfVarsFilePresent() throws Exception {
    PcfManifestsPackage manifestsPackage = PcfManifestsPackage.builder().build();
    PcfCommandSetupRequest setupRequest =
        PcfCommandSetupRequest.builder().pcfManifestsPackage(manifestsPackage).build();
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();

    manifestsPackage.setVariableYmls(emptyList());
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();

    String str = null;
    manifestsPackage.setVariableYmls(Arrays.asList(str));
    assertThat(pcfSetupCommandTaskHandler.checkIfVarsFilePresent(setupRequest)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testPrepareVarsYamlFile() throws Exception {
    File f1 = mock(File.class);
    File f2 = mock(File.class);
    doReturn(f1)
        .doReturn(f2)
        .when(pcfCommandTaskHelper)
        .createManifestVarsYamlFileLocally(any(), anyString(), anyInt());

    PcfCreateApplicationRequestData requestData =
        PcfCreateApplicationRequestData.builder()
            .setupRequest(PcfCommandSetupRequest.builder()
                              .pcfManifestsPackage(
                                  PcfManifestsPackage.builder().variableYmls(Arrays.asList("a:b", "c:d")).build())
                              .releaseNamePrefix("abc")
                              .build())
            .varsYmlFilePresent(true)
            .pcfManifestFileData(PcfManifestFileData.builder().varFiles(new ArrayList<>()).build())
            .build();

    pcfSetupCommandTaskHandler.prepareVarsYamlFile(requestData);

    assertThat(requestData.getPcfManifestFileData()).isNotNull();
    assertThat(requestData.getPcfManifestFileData().getVarFiles()).isNotEmpty();
    assertThat(requestData.getPcfManifestFileData().getVarFiles().size()).isEqualTo(2);
    assertThat(requestData.getPcfManifestFileData().getVarFiles()).containsExactly(f1, f2);
  }
}
