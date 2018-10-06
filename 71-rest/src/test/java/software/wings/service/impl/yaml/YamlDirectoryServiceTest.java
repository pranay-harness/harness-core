package software.wings.service.impl.yaml;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.security.AppPermissionSummary;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryNode.NodeType;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.YamlNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class YamlDirectoryServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ConfigService configService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;
  @Mock private InfrastructureProvisionerService provisionerService;
  @Inject @InjectMocks private YamlDirectoryService yamlDirectoryService;

  @Test
  public void testDoApplications() throws Exception {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
    Map<String, AppPermissionSummary> appPermissionSummaryMap = new HashMap<>();
    appPermissionSummaryMap.put(APP_ID, null);
    performMocking();

    DirectoryNode directoryNode = (DirectoryNode) MethodUtils.invokeMethod(yamlDirectoryService, true, "doApplications",
        new Object[] {ACCOUNT_ID, directoryPath, false, appPermissionSummaryMap});

    assertNotNull(directoryNode);
    FolderNode folderNode = (FolderNode) directoryNode;
    assertNotNull(folderNode.getChildren());
    assertEquals(1, folderNode.getChildren().size());

    FolderNode appNode = (FolderNode) folderNode.getChildren().get(0);
    assertEquals(APP_ID, appNode.getAppId());
    assertEquals(ACCOUNT_ID, appNode.getAccountId());
    assertEquals("Application", appNode.getShortClassName());

    for (DirectoryNode node : appNode.getChildren()) {
      assertEquals(ACCOUNT_ID, node.getAccountId());

      if (node.getName().equals("Index.yaml")) {
        assertEquals("Setup/Applications/APP_NAME/Index.yaml", node.getDirectoryPath().getPath());
        YamlNode yamlNode = (YamlNode) node;
        assertEquals(APP_ID, yamlNode.getUuid());
        assertEquals(NodeType.YAML, yamlNode.getType());
      } else if (node.getName().equals("Defaults.yaml")) {
        assertEquals("Setup/Applications/APP_NAME/Defaults.yaml", node.getDirectoryPath().getPath());
        YamlNode yamlNode = (YamlNode) node;
        assertEquals(APP_ID, yamlNode.getUuid());
        assertEquals(NodeType.YAML, yamlNode.getType());
      } else if (node.getName().equals("Services")) {
        FolderNode serviceFolderNode =
            validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Services");
        performServiceNodeValidation(serviceFolderNode);

      } else if (node.getName().equals("Environments")) {
        FolderNode envFolderNode =
            validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Environments");
        performEnvironmentNodeValidation(envFolderNode);

      } else if (node.getName().equals("Workflows")) {
        FolderNode workflowNode =
            validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Workflows");
        performWorkflowNodeValidation(workflowNode);

      } else if (node.getName().equals("Pipelines")) {
        FolderNode pipelineFolderNode =
            validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Pipelines");
        performPipelineNodeValidation(pipelineFolderNode);
      } else if (node.getName().equals("Provisioners")) {
        FolderNode provisionerFolderNode =
            validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Provisioners");
        performProvisionerNodeValidation(provisionerFolderNode);
      }
    }
  }

  private void performWorkflowNodeValidation(FolderNode workflowNode) {
    assertNotNull(workflowNode.getChildren());
    assertEquals(1, workflowNode.getChildren().size());
    AppLevelYamlNode workflowYamlNode = (AppLevelYamlNode) workflowNode.getChildren().get(0);
    assertEquals(
        "Setup/Applications/APP_NAME/Workflows/WORKFLOW_NAME.yaml", workflowYamlNode.getDirectoryPath().getPath());
    assertEquals(APP_ID, workflowYamlNode.getAppId());
    assertEquals(ACCOUNT_ID, workflowYamlNode.getAccountId());
    assertEquals(WORKFLOW_ID, workflowYamlNode.getUuid());
    assertEquals("WORKFLOW_NAME.yaml", workflowYamlNode.getName());
  }

  private void performPipelineNodeValidation(FolderNode pipelineNode) {
    assertNotNull(pipelineNode.getChildren());
    assertEquals(1, pipelineNode.getChildren().size());
    AppLevelYamlNode pipelineYamlNode = (AppLevelYamlNode) pipelineNode.getChildren().get(0);
    assertEquals(
        "Setup/Applications/APP_NAME/Pipelines/PIPELINE_NAME.yaml", pipelineYamlNode.getDirectoryPath().getPath());
    assertEquals(APP_ID, pipelineYamlNode.getAppId());
    assertEquals(ACCOUNT_ID, pipelineYamlNode.getAccountId());
    assertEquals(PIPELINE_ID, pipelineYamlNode.getUuid());
    assertEquals("PIPELINE_NAME.yaml", pipelineYamlNode.getName());
  }

  private void performProvisionerNodeValidation(FolderNode provisionerFolderNode) {
    assertNotNull(provisionerFolderNode.getChildren());
    assertEquals(1, provisionerFolderNode.getChildren().size());
    AppLevelYamlNode provisionerYamlNode = (AppLevelYamlNode) provisionerFolderNode.getChildren().get(0);
    assertEquals("Setup/Applications/APP_NAME/Provisioners/PROVISIONER_NAME.yaml",
        provisionerYamlNode.getDirectoryPath().getPath());
    assertEquals(APP_ID, provisionerYamlNode.getAppId());
    assertEquals(ACCOUNT_ID, provisionerYamlNode.getAccountId());
    assertEquals(PROVISIONER_ID, provisionerYamlNode.getUuid());
    assertEquals("PROVISIONER_NAME.yaml", provisionerYamlNode.getName());
  }

  private void performEnvironmentNodeValidation(FolderNode envFolderNode) {
    assertNotNull(envFolderNode.getChildren());
    assertEquals(1, envFolderNode.getChildren().size());

    // This is actual Service Folder for service with Name "SERVICE_NAME"
    assertEquals(ENV_NAME, envFolderNode.getChildren().get(0).getName());
    assertEquals(NodeType.FOLDER, envFolderNode.getChildren().get(0).getType());

    // These are nested yaml strcutures for service like index.yaml, config files, commands etc.
    Set<String> expectedDirPaths =
        new HashSet<>(Arrays.asList("Setup/Applications/APP_NAME/Environments/ENV_NAME/Index.yaml",
            "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure",
            "Setup/Applications/APP_NAME/Environments/ENV_NAME/Config Files"));

    assertEquals(3, ((FolderNode) envFolderNode.getChildren().get(0)).getChildren().size());
    for (DirectoryNode envChildNode : ((FolderNode) envFolderNode.getChildren().get(0)).getChildren()) {
      assertTrue(expectedDirPaths.contains(envChildNode.getDirectoryPath().getPath()));
      expectedDirPaths.remove(envChildNode.getDirectoryPath().getPath());
    }

    assertEquals(0, expectedDirPaths.size());
  }

  private void performServiceNodeValidation(FolderNode serviceFolderNode) {
    assertNotNull(serviceFolderNode.getChildren());
    assertEquals(1, serviceFolderNode.getChildren().size());

    // This is actual Service Folder for service with Name "SERVICE_NAME"
    assertEquals(SERVICE_NAME, serviceFolderNode.getChildren().get(0).getName());
    assertEquals(NodeType.FOLDER, serviceFolderNode.getChildren().get(0).getType());

    // These are nested yaml strcutures for service like index.yaml, config files, commands etc.
    Set<String> expectedDirPaths =
        new HashSet<>(Arrays.asList("Setup/Applications/APP_NAME/Services/SERVICE_NAME/Index.yaml",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Commands",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Deployment Specifications",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Artifact Servers",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Config Files"));

    assertEquals(5, ((FolderNode) serviceFolderNode.getChildren().get(0)).getChildren().size());
    for (DirectoryNode serviceChildNode : ((FolderNode) serviceFolderNode.getChildren().get(0)).getChildren()) {
      assertTrue(expectedDirPaths.contains(serviceChildNode.getDirectoryPath().getPath()));
      expectedDirPaths.remove(serviceChildNode.getDirectoryPath().getPath());
    }

    assertEquals(0, expectedDirPaths.size());
  }

  private void performMocking() {
    Application application = anApplication().withUuid(APP_ID).withName(APP_NAME).withAccountId(ACCOUNT_ID).build();

    doReturn(Arrays.asList(application)).when(appService).getAppsByAccountId(anyString());

    doReturn(application).when(appService).get(anyString());

    doReturn(
        Arrays.asList(
            Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).appId(APP_ID).artifactType(ArtifactType.PCF).build()))
        .when(serviceResourceService)
        .findServicesByApp(anyString());

    doReturn(false).when(serviceResourceService).hasInternalCommands(any());

    doReturn(PcfServiceSpecification.builder().serviceId(SERVICE_ID).manifestYaml("Fake Manifest.yaml").build())
        .when(serviceResourceService)
        .getPcfServiceSpecification(anyString(), anyString());

    doReturn(Collections.EMPTY_LIST).when(artifactStreamService).getArtifactStreamsForService(any(), any());

    doReturn(Collections.EMPTY_LIST).when(configService).getConfigFilesForEntity(anyString(), anyString(), anyString());

    doReturn(Arrays.asList(anEnvironment().withName(ENV_NAME).withUuid(ENV_ID).withAppId(APP_ID).build()))
        .when(environmentService)
        .getEnvByApp(anyString());

    PageResponse<InfrastructureMapping> mappingResponse =
        aPageResponse()
            .withResponse(Arrays.asList(PcfInfrastructureMapping.builder()
                                            .organization("ORG")
                                            .space("SPACE")
                                            .routeMaps(Arrays.asList("url1.com"))
                                            .accountId(ACCOUNT_ID)
                                            .appId(APP_ID)
                                            .infraMappingType(InfrastructureMappingType.PCF_PCF.name())
                                            .envId(ENV_ID)
                                            .build()))
            .build();

    doReturn(mappingResponse).when(infraMappingService).list(any());

    doReturn(Collections.EMPTY_LIST).when(configService).getConfigFileOverridesForEnv(anyString(), anyString());

    PageResponse<Workflow> workflowsResponse = aPageResponse()
                                                   .withResponse(Arrays.asList(aWorkflow()
                                                                                   .withAppId(APP_ID)
                                                                                   .withName(WORKFLOW_NAME)
                                                                                   .withEnvId(ENV_ID)
                                                                                   .withServiceId(SERVICE_ID)
                                                                                   .withInfraMappingId(INFRA_MAPPING_ID)
                                                                                   .withUuid(WORKFLOW_ID)
                                                                                   .build()))
                                                   .build();
    doReturn(workflowsResponse).when(workflowService).listWorkflows(any());

    PageResponse<Pipeline> pipelineResponse =
        aPageResponse()
            .withResponse(Arrays.asList(Pipeline.builder().appId(APP_ID).uuid(PIPELINE_ID).name(PIPELINE_NAME).build()))
            .build();
    doReturn(pipelineResponse).when(pipelineService).listPipelines(any());

    PageResponse<InfrastructureProvisioner> provisionerResponse =
        aPageResponse()
            .withResponse(Arrays.asList(CloudFormationInfrastructureProvisioner.builder()
                                            .appId(APP_ID)
                                            .uuid(PROVISIONER_ID)
                                            .name(PROVISIONER_NAME)
                                            .build()))
            .build();
    PageResponse<InfrastructureProvisioner> response = aPageResponse().withResponse(provisionerResponse).build();
    doReturn(response).when(provisionerService).list(any());
  }

  private FolderNode validateFolderNodeGotAppAccId(FolderNode node, String dirPath) {
    FolderNode currentFolderNode = node;
    assertEquals(NodeType.FOLDER, currentFolderNode.getType());
    assertEquals(APP_ID, currentFolderNode.getAppId());
    assertEquals(dirPath, currentFolderNode.getDirectoryPath().getPath());
    return currentFolderNode;
  }
}
