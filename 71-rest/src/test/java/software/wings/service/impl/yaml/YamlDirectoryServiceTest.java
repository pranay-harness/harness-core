package software.wings.service.impl.yaml;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
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
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
  @Inject @InjectMocks private YamlDirectoryServiceImpl yamlDirectoryService;

  @Test
  @Category(UnitTests.class)
  public void testDoApplications() throws Exception {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
    Map<String, AppPermissionSummary> appPermissionSummaryMap = new HashMap<>();
    appPermissionSummaryMap.put(APP_ID, null);
    performMocking();
    FolderNode directoryNode =
        yamlDirectoryService.doApplications(ACCOUNT_ID, directoryPath, false, appPermissionSummaryMap);
    assertThat(directoryNode).isNotNull();
    assertThat(directoryNode.getChildren()).isNotNull();
    assertThat(directoryNode.getChildren()).hasSize(1);

    FolderNode appNode = (FolderNode) directoryNode.getChildren().get(0);
    assertThat(appNode.getAppId()).isEqualTo(APP_ID);
    assertThat(appNode.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(appNode.getShortClassName()).isEqualTo("Application");

    for (DirectoryNode node : appNode.getChildren()) {
      assertThat(node.getAccountId()).isEqualTo(ACCOUNT_ID);

      switch (node.getName()) {
        case "Index.yaml": {
          assertThat(node.getDirectoryPath().getPath()).isEqualTo("Setup/Applications/APP_NAME/Index.yaml");
          YamlNode yamlNode = (YamlNode) node;
          assertThat(yamlNode.getUuid()).isEqualTo(APP_ID);
          assertThat(yamlNode.getType()).isEqualTo(NodeType.YAML);
          break;
        }
        case "Defaults.yaml": {
          assertThat(node.getDirectoryPath().getPath()).isEqualTo("Setup/Applications/APP_NAME/Defaults.yaml");
          YamlNode yamlNode = (YamlNode) node;
          assertThat(yamlNode.getUuid()).isEqualTo(APP_ID);
          assertThat(yamlNode.getType()).isEqualTo(NodeType.YAML);
          break;
        }
        case "Services":
          FolderNode serviceFolderNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Services");
          performServiceNodeValidation(serviceFolderNode);
          break;
        case "Environments":
          FolderNode envFolderNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Environments");
          performEnvironmentNodeValidation(envFolderNode);
          break;
        case "Workflows":
          FolderNode workflowNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Workflows");
          performWorkflowNodeValidation(workflowNode);
          break;
        case "Pipelines":
          FolderNode pipelineFolderNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Pipelines");
          performPipelineNodeValidation(pipelineFolderNode);
          break;
        case "Provisioners":
          FolderNode provisionerFolderNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Provisioners");
          performProvisionerNodeValidation(provisionerFolderNode);
          break;
        case "Triggers":
          FolderNode triggerFolderNode =
              validateFolderNodeGotAppAccId((FolderNode) node, "Setup/Applications/APP_NAME/Triggers");
          performTriggerNodeValidation(triggerFolderNode);
          break;
        default:
          throw new IllegalArgumentException("Unknown node name: " + node.getName());
      }
    }
  }

  private void performWorkflowNodeValidation(FolderNode workflowNode) {
    assertThat(workflowNode.getChildren()).isNotNull();
    assertThat(workflowNode.getChildren()).hasSize(1);
    AppLevelYamlNode workflowYamlNode = (AppLevelYamlNode) workflowNode.getChildren().get(0);
    assertEquals(
        "Setup/Applications/APP_NAME/Workflows/WORKFLOW_NAME.yaml", workflowYamlNode.getDirectoryPath().getPath());
    assertThat(workflowYamlNode.getAppId()).isEqualTo(APP_ID);
    assertThat(workflowYamlNode.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(workflowYamlNode.getUuid()).isEqualTo(WORKFLOW_ID);
    assertThat(workflowYamlNode.getName()).isEqualTo("WORKFLOW_NAME.yaml");
  }

  private void performPipelineNodeValidation(FolderNode pipelineNode) {
    assertThat(pipelineNode.getChildren()).isNotNull();
    assertThat(pipelineNode.getChildren()).hasSize(1);
    AppLevelYamlNode pipelineYamlNode = (AppLevelYamlNode) pipelineNode.getChildren().get(0);
    assertEquals(
        "Setup/Applications/APP_NAME/Pipelines/PIPELINE_NAME.yaml", pipelineYamlNode.getDirectoryPath().getPath());
    assertThat(pipelineYamlNode.getAppId()).isEqualTo(APP_ID);
    assertThat(pipelineYamlNode.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(pipelineYamlNode.getUuid()).isEqualTo(PIPELINE_ID);
    assertThat(pipelineYamlNode.getName()).isEqualTo("PIPELINE_NAME.yaml");
  }

  private void performProvisionerNodeValidation(FolderNode provisionerFolderNode) {
    assertThat(provisionerFolderNode.getChildren()).isNotNull();
    assertThat(provisionerFolderNode.getChildren()).hasSize(1);
    AppLevelYamlNode provisionerYamlNode = (AppLevelYamlNode) provisionerFolderNode.getChildren().get(0);
    assertEquals("Setup/Applications/APP_NAME/Provisioners/PROVISIONER_NAME.yaml",
        provisionerYamlNode.getDirectoryPath().getPath());
    assertThat(provisionerYamlNode.getAppId()).isEqualTo(APP_ID);
    assertThat(provisionerYamlNode.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(provisionerYamlNode.getUuid()).isEqualTo(PROVISIONER_ID);
    assertThat(provisionerYamlNode.getName()).isEqualTo("PROVISIONER_NAME.yaml");
  }
  private void performTriggerNodeValidation(FolderNode triggerFolderNode) {
    assertThat(triggerFolderNode.getChildren()).isNotNull();
    assertThat(triggerFolderNode.getChildren()).hasSize(1);
    AppLevelYamlNode triggerYamlNode = (AppLevelYamlNode) triggerFolderNode.getChildren().get(0);
    assertEquals(
        "Setup/Applications/APP_NAME/Triggers/TRIGGER_NAME.yaml", triggerYamlNode.getDirectoryPath().getPath());
    assertThat(triggerYamlNode.getAppId()).isEqualTo(APP_ID);
    assertThat(triggerYamlNode.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(triggerYamlNode.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(triggerYamlNode.getName()).isEqualTo("TRIGGER_NAME.yaml");
  }

  private void performEnvironmentNodeValidation(FolderNode envFolderNode) {
    assertThat(envFolderNode.getChildren()).isNotNull();
    assertThat(envFolderNode.getChildren()).hasSize(1);

    // This is actual Service Folder for service with Name "SERVICE_NAME"
    assertThat(envFolderNode.getChildren().get(0).getName()).isEqualTo(ENV_NAME);
    assertThat(envFolderNode.getChildren().get(0).getType()).isEqualTo(NodeType.FOLDER);

    // These are nested yaml strcutures for service like index.yaml, config files, commands etc.
    Set<String> expectedDirPaths =
        new HashSet<>(Arrays.asList("Setup/Applications/APP_NAME/Environments/ENV_NAME/Index.yaml",
            "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure",
            "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Verification",
            "Setup/Applications/APP_NAME/Environments/ENV_NAME/Config Files"));

    assertThat(((FolderNode) envFolderNode.getChildren().get(0)).getChildren()).hasSize(expectedDirPaths.size());
    for (DirectoryNode envChildNode : ((FolderNode) envFolderNode.getChildren().get(0)).getChildren()) {
      assertThat(expectedDirPaths.contains(envChildNode.getDirectoryPath().getPath())).isTrue();
      expectedDirPaths.remove(envChildNode.getDirectoryPath().getPath());
    }

    assertThat(expectedDirPaths).isEmpty();
  }

  private void performServiceNodeValidation(FolderNode serviceFolderNode) {
    assertThat(serviceFolderNode.getChildren()).isNotNull();
    assertThat(serviceFolderNode.getChildren()).hasSize(1);

    // This is actual Service Folder for service with Name "SERVICE_NAME"
    assertThat(serviceFolderNode.getChildren().get(0).getName()).isEqualTo(SERVICE_NAME);
    assertThat(serviceFolderNode.getChildren().get(0).getType()).isEqualTo(NodeType.FOLDER);

    // These are nested yaml structures for service like index.yaml, config files, commands etc.
    Set<String> expectedDirPaths =
        new HashSet<>(Arrays.asList("Setup/Applications/APP_NAME/Services/SERVICE_NAME/Index.yaml",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Commands",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Deployment Specifications",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Artifact Servers",
            "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Config Files"));

    assertThat(((FolderNode) serviceFolderNode.getChildren().get(0)).getChildren()).hasSize(expectedDirPaths.size());
    for (DirectoryNode serviceChildNode : ((FolderNode) serviceFolderNode.getChildren().get(0)).getChildren()) {
      assertThat(expectedDirPaths.contains(serviceChildNode.getDirectoryPath().getPath())).isTrue();
      expectedDirPaths.remove(serviceChildNode.getDirectoryPath().getPath());
    }

    assertThat(expectedDirPaths).isEmpty();
  }

  private void performMocking() {
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();

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

    doReturn(Arrays.asList(anEnvironment().name(ENV_NAME).uuid(ENV_ID).appId(APP_ID).build()))
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
                                                                                   .appId(APP_ID)
                                                                                   .name(WORKFLOW_NAME)
                                                                                   .envId(ENV_ID)
                                                                                   .serviceId(SERVICE_ID)
                                                                                   .infraMappingId(INFRA_MAPPING_ID)
                                                                                   .uuid(WORKFLOW_ID)
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
    assertThat(currentFolderNode.getType()).isEqualTo(NodeType.FOLDER);
    assertThat(currentFolderNode.getAppId()).isEqualTo(APP_ID);
    assertThat(currentFolderNode.getDirectoryPath().getPath()).isEqualTo(dirPath);
    return currentFolderNode;
  }
}
