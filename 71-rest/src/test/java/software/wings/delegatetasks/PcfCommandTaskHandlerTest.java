package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfApplicationDetailsCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDataFetchCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfDeployCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfRollbackCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfSetupCommandTaskHandler;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.helpers.ext.pcf.response.PcfInfraMappingDataResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
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
      + "      - route: ${ROUTE_MAP}\n"
      + "serviceName: SERV\n";
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
  @InjectMocks @Inject PcfRollbackCommandTaskHandler pcfRollbackCommandTaskHandler;
  @InjectMocks @Inject PcfDataFetchCommandTaskHandler pcfDataFetchCommandTaskHandler;
  @InjectMocks @Inject PcfApplicationDetailsCommandTaskHandler pcfApplicationDetailsCommandTaskHandler;

  @Test
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

    doNothing().when(pcfDeploymentManager).deleteApplication(any());
    File f1 = new File("./test1");
    File f2 = new File("./test2");
    doReturn(f1).when(pcfCommandTaskHelper).downloadArtifact(any(), any());
    doReturn(f2).when(pcfCommandTaskHelper).createManifestYamlFileLocally(any(), any(), any());
    doNothing().when(pcfCommandTaskHelper).deleteCreataedFile(anyList());

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
    verify(pcfDeploymentManager, times(2)).deleteApplication(any());

    PcfSetupCommandResponse pcfSetupCommandResponse =
        (PcfSetupCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertEquals(pcfCommandExecutionResponse.getCommandExecutionStatus(), CommandExecutionStatus.SUCCESS);
    assertNotNull(pcfSetupCommandResponse.getNewApplicationDetails());
    assertEquals(pcfSetupCommandResponse.getNewApplicationDetails().getApplicationName(), "a_s_e__6");
    assertEquals(pcfSetupCommandResponse.getNewApplicationDetails().getApplicationGuid(), "10");

    assertNotNull(pcfSetupCommandResponse.getDownsizeDetails());
    assertEquals(2, pcfSetupCommandResponse.getDownsizeDetails().size());
    Set<String> appsToBeDownsized = new HashSet<>(
        pcfSetupCommandResponse.getDownsizeDetails().stream().map(app -> app.getApplicationName()).collect(toList()));
    assertTrue(appsToBeDownsized.contains("a_s_e__3"));
    assertTrue(appsToBeDownsized.contains("a_s_e__4"));
  }

  private PcfConfig getPcfConfig() {
    return PcfConfig.builder().username(USERNMAE).endpointUrl(URL).password(new char[0]).build();
  }

  @Test
  public void testPerformDeploy_nonBlueGreen() throws Exception {
    PcfCommandRequest pcfCommandRequest = PcfCommandDeployRequest.builder()
                                              .pcfCommandType(PcfCommandType.RESIZE)
                                              .resizeStrategy(ResizeStrategy.DOWNSIZE_OLD_FIRST)
                                              .pcfConfig(getPcfConfig())
                                              .accountId(ACCOUNT_ID)
                                              .newReleaseName("a_s_e__6")
                                              .organization(ORG)
                                              .space(SPACE)
                                              .updateCount(2)
                                              .downSizeCount(2)
                                              .totalPreviousInstanceCount(2)
                                              .timeoutIntervalInMin(2)
                                              .build();

    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("10")
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name("a_s_e__6")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(0)
                                              .build();

    doReturn(applicationDetail)
        .doReturn(applicationDetail)
        .doReturn(applicationDetail)
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
                             .instances(1)
                             .memoryLimit(1)
                             .runningInstances(1)
                             .build());
    previousReleases.add(ApplicationSummary.builder()
                             .name("a_s_e__3")
                             .diskQuota(1)
                             .requestedState(RUNNING)
                             .id("1")
                             .instances(1)
                             .memoryLimit(1)
                             .runningInstances(1)
                             .build());

    doReturn(previousReleases).when(pcfDeploymentManager).getPreviousReleases(any(), anyString());
    doReturn(previousReleases).when(pcfDeploymentManager).getDeployedServicesWithNonZeroInstances(any(), anyString());

    doReturn(ApplicationDetail.builder()
                 .id("10")
                 .diskQuota(1)
                 .instances(0)
                 .memoryLimit(1)
                 .name("a_s_e__4")
                 .requestedState("STOPPED")
                 .stack("")
                 .runningInstances(0)
                 .build())
        .doReturn(ApplicationDetail.builder()
                      .id("10")
                      .diskQuota(1)
                      .instances(0)
                      .memoryLimit(1)
                      .name("a_s_e__3")
                      .requestedState("STOPPED")
                      .stack("")
                      .runningInstances(0)
                      .build())
        .doReturn(ApplicationDetail.builder()
                      .id("10")
                      .diskQuota(1)
                      .instances(2)
                      .memoryLimit(1)
                      .name("a_s_e__4")
                      .requestedState("STOPPED")
                      .stack("")
                      .runningInstances(2)
                      .build())
        .when(pcfDeploymentManager)
        .resizeApplication(any());

    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfDeployCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null);

    assertEquals(CommandExecutionStatus.SUCCESS, pcfCommandExecutionResponse.getCommandExecutionStatus());
    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertEquals(CommandExecutionStatus.SUCCESS, pcfDeployCommandResponse.getCommandExecutionStatus());
    List<PcfServiceData> pcfServiceDatas = pcfDeployCommandResponse.getInstanceDataUpdated();
    assertEquals(3, pcfServiceDatas.size());
    for (PcfServiceData data : pcfServiceDatas) {
      if (data.getName().equals("a_s_e__4")) {
        assertEquals(1, data.getPreviousCount());
        assertEquals(0, data.getDesiredCount());
      } else if (data.getName().equals("a_s_e__3")) {
        assertEquals(1, data.getPreviousCount());
        assertEquals(0, data.getDesiredCount());
      } else if (data.getName().equals("a_s_e__6")) {
        assertEquals(0, data.getPreviousCount());
        assertEquals(2, data.getDesiredCount());
      }
    }
  }

  @Test
  public void testPerformRollback() throws Exception {
    PcfCommandRequest pcfCommandRequest =
        PcfCommandRollbackRequest.builder()
            .pcfCommandType(PcfCommandType.ROLLBACK)
            .pcfConfig(getPcfConfig())
            .accountId(ACCOUNT_ID)
            .instanceData(
                Arrays.asList(PcfServiceData.builder().name("a_s_e__6").previousCount(2).desiredCount(0).build(),
                    PcfServiceData.builder().name("a_s_e__4").previousCount(0).desiredCount(1).build(),
                    PcfServiceData.builder().name("a_s_e__3").previousCount(0).desiredCount(1).build()))
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
                 .name("a_s_e__4")
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
        .doReturn(ApplicationDetail.builder()
                      .id("Guid:a_s_e__3")
                      .diskQuota(1)
                      .instances(1)
                      .memoryLimit(1)
                      .name("a_s_e__3")
                      .requestedState("STOPPED")
                      .stack("")
                      .runningInstances(0)
                      .build())
        .when(pcfDeploymentManager)
        .getApplicationByName(any());

    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .id("Guid:a_s_e__6")
                                              .diskQuota(1)
                                              .instances(0)
                                              .memoryLimit(1)
                                              .name("a_s_e__6")
                                              .requestedState("STOPPED")
                                              .stack("")
                                              .runningInstances(0)
                                              .build();

    doReturn(applicationDetail)
        .doReturn(ApplicationDetail.builder()
                      .instanceDetails(Arrays.asList(InstanceDetail.builder()
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
        // 3rd time is upsize
        .doReturn(ApplicationDetail.builder()
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
        .resizeApplication(any());

    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfRollbackCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null);

    assertEquals(CommandExecutionStatus.SUCCESS, pcfCommandExecutionResponse.getCommandExecutionStatus());
    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertEquals(CommandExecutionStatus.SUCCESS, pcfDeployCommandResponse.getCommandExecutionStatus());
    assertNotNull(pcfDeployCommandResponse.getPcfInstanceElements());
    assertEquals(2, pcfDeployCommandResponse.getPcfInstanceElements().size());

    Set<String> pcfInstanceElements = new HashSet<>();
    ((PcfDeployCommandResponse) pcfCommandExecutionResponse.getPcfCommandResponse())
        .getPcfInstanceElements()
        .forEach(pcfInstanceElement
            -> pcfInstanceElements.add(
                pcfInstanceElement.getApplicationId() + ":" + pcfInstanceElement.getInstanceIndex()));
    assertTrue(pcfInstanceElements.contains("Guid:a_s_e__3:2"));
    assertTrue(pcfInstanceElements.contains("Guid:a_s_e__4:1"));
  }

  @Test
  public void testperformDataFetch() throws Exception {
    PcfInfraMappingDataRequest pcfCommandRequest = PcfInfraMappingDataRequest.builder()
                                                       .pcfCommandType(PcfCommandType.DATAFETCH)
                                                       .pcfConfig(getPcfConfig())
                                                       .accountId(ACCOUNT_ID)
                                                       .timeoutIntervalInMin(5)
                                                       .build();

    doReturn(Arrays.asList(ORG)).when(pcfDeploymentManager).getOrganizations(any());
    doReturn(Arrays.asList(SPACE)).when(pcfDeploymentManager).getSpacesForOrganization(any());
    doReturn(Arrays.asList("R1", "R2")).when(pcfDeploymentManager).getRouteMaps(any());

    // Fetch Orgs
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null);

    assertEquals(CommandExecutionStatus.SUCCESS, pcfCommandExecutionResponse.getCommandExecutionStatus());
    PcfInfraMappingDataResponse pcfInfraMappingDataResponse =
        (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertEquals(CommandExecutionStatus.SUCCESS, pcfInfraMappingDataResponse.getCommandExecutionStatus());
    assertNotNull(pcfInfraMappingDataResponse.getOrganizations());
    assertEquals(1, pcfInfraMappingDataResponse.getOrganizations().size());
    assertEquals(ORG, pcfInfraMappingDataResponse.getOrganizations().get(0));

    // Fetch Spaces for org
    pcfCommandRequest.setOrganization(ORG);
    pcfCommandExecutionResponse = pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null);

    assertEquals(CommandExecutionStatus.SUCCESS, pcfCommandExecutionResponse.getCommandExecutionStatus());
    pcfInfraMappingDataResponse = (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertNotNull(pcfInfraMappingDataResponse.getSpaces());
    assertEquals(1, pcfInfraMappingDataResponse.getSpaces().size());
    assertEquals(SPACE, pcfInfraMappingDataResponse.getSpaces().get(0));

    // Fetch Routes
    pcfCommandRequest.setSpace(SPACE);
    pcfCommandExecutionResponse = pcfDataFetchCommandTaskHandler.executeTaskInternal(pcfCommandRequest, null);
    pcfInfraMappingDataResponse = (PcfInfraMappingDataResponse) pcfCommandExecutionResponse.getPcfCommandResponse();
    assertNotNull(pcfInfraMappingDataResponse.getRouteMaps());
    assertEquals(2, pcfInfraMappingDataResponse.getRouteMaps().size());
    assertTrue(pcfInfraMappingDataResponse.getRouteMaps().contains("R1"));
    assertTrue(pcfInfraMappingDataResponse.getRouteMaps().contains("R2"));
  }

  @Test
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
        pcfApplicationDetailsCommandTaskHandler.executeTaskInternal(pcfInstanceSyncRequest, null);

    assertEquals(CommandExecutionStatus.SUCCESS, pcfCommandExecutionResponse.getCommandExecutionStatus());
    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        (PcfInstanceSyncResponse) pcfCommandExecutionResponse.getPcfCommandResponse();

    assertEquals(CommandExecutionStatus.SUCCESS, pcfInstanceSyncResponse.getCommandExecutionStatus());
    assertNotNull(pcfInstanceSyncResponse.getInstanceIndices());
    assertEquals(1, pcfInstanceSyncResponse.getInstanceIndices().size());
    assertEquals("2", pcfInstanceSyncResponse.getInstanceIndices().get(0));
  }
}
