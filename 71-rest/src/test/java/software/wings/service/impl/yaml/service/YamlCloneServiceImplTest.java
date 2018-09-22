package software.wings.service.impl.yaml.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.clone.YamlCloneService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class YamlCloneServiceImplTest extends WingsBaseTest {
  private static final String YAML_EXTENSION = ".yaml";
  public static final String NEW_WORKFLOW_NAME = "newWorkflowName";
  public static final String NEW_PIPELINE_NAME = "newPipelineName";
  public static final String NEW_PROVISIONER_NAME = "newProvisionerName";
  @Mock private YamlGitService yamlGitSyncService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;
  @Mock private InfrastructureProvisionerService provisionerService;
  @Mock private AppService appService;
  @Mock private YamlService yamlService;
  @InjectMocks @Inject private YamlCloneService yamlCloneService;
  private YamlCloneServiceImpl yamlCloneServiceImpl;
  private List<GitFileChange> gitFileChangeList;
  public static final String NEW_PATH_FOR_WORKFLOW = new StringBuilder(64)
                                                         .append("Setup/Applications/")
                                                         .append(APP_NAME)
                                                         .append("/Workflows/")
                                                         .append(NEW_WORKFLOW_NAME)
                                                         .append(YAML_EXTENSION)
                                                         .toString();
  public static final String NEW_PATH_FOR_PIPELINES = new StringBuilder(64)
                                                          .append("Setup/Applications/")
                                                          .append(APP_NAME)
                                                          .append("/Pipelines/")
                                                          .append(NEW_PIPELINE_NAME)
                                                          .append(YAML_EXTENSION)
                                                          .toString();
  public static final String NEW_PATH_FOR_PROVIOSIONER = new StringBuilder(64)
                                                             .append("Setup/Applications/")
                                                             .append(APP_NAME)
                                                             .append("/Provisioners/")
                                                             .append(NEW_PROVISIONER_NAME)
                                                             .append(YAML_EXTENSION)
                                                             .toString();

  @Before
  public void setup() throws Exception {
    yamlCloneServiceImpl = (YamlCloneServiceImpl) yamlCloneService;
    gitFileChangeList = new ArrayList<>();
    doReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).withName(APP_NAME).build())
        .when(appService)
        .get(anyString());
  }

  @Test
  public void testTraverseDirectory() throws Exception {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
    directoryPath.add(APPLICATIONS_FOLDER);
    directoryPath.add(APP_NAME);
    FolderNode workflowsFolder =
        new FolderNode(ACCOUNT_ID, WORKFLOWS_FOLDER, Workflow.class, directoryPath, APP_ID, yamlGitSyncService);

    DirectoryPath workflowPath = directoryPath.clone();
    String workflowYamlFileName = NEW_WORKFLOW_NAME + YAML_EXTENSION;
    workflowsFolder.addChild(new AppLevelYamlNode(ACCOUNT_ID, WORKFLOW_ID, APP_ID, workflowYamlFileName, Workflow.class,
        workflowPath.add(workflowYamlFileName), yamlGitSyncService, Type.WORKFLOW));

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        gitFileChangeList = (List<GitFileChange>) args[4];
        gitFileChangeList.add(GitFileChange.Builder.aGitFileChange()
                                  .withFilePath(NEW_PATH_FOR_WORKFLOW)
                                  .withAccountId(ACCOUNT_ID)
                                  .withFileContent("")
                                  .build());
        return StringUtils.EMPTY;
      }
    })
        .when(yamlDirectoryService)
        .getGitFileChange(any(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean());

    Optional<List<String>> errorMessages = Optional.of(new ArrayList<>());
    MethodUtils.invokeMethod(yamlCloneServiceImpl, true, "traverseDirectory",
        new Object[] {gitFileChangeList, ACCOUNT_ID, workflowsFolder, workflowsFolder.getDirectoryPath().getPath(),
            false, errorMessages});

    assertEquals(1, gitFileChangeList.size());
    assertEquals(ACCOUNT_ID, gitFileChangeList.get(0).getAccountId());
    assertEquals(NEW_PATH_FOR_WORKFLOW, gitFileChangeList.get(0).getFilePath());

    verify(yamlDirectoryService, times(1))
        .getGitFileChange(any(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean());
  }

  @Test
  public void testCloneEntityUsingYaml_Workflow() throws Exception {
    doReturn(null).when(workflowService).readWorkflowByName(anyString(), anyString());
    doReturn(aWorkflow().withAppId(APP_ID).withUuid(WORKFLOW_ID).build())
        .when(workflowService)
        .readWorkflow(anyString(), anyString());

    testEntityClone(NEW_PATH_FOR_WORKFLOW, EntityType.WORKFLOW, WORKFLOW_ID, NEW_WORKFLOW_NAME);
  }

  @Test
  public void testCloneEntityUsingYaml_Pipeline() throws Exception {
    doReturn(null).when(pipelineService).getPipelineByName(anyString(), anyString());
    doReturn(Pipeline.builder().appId(APP_ID).uuid(PIPELINE_ID).build())
        .when(pipelineService)
        .readPipeline(anyString(), anyString(), anyBoolean());

    testEntityClone(NEW_PATH_FOR_PIPELINES, EntityType.PIPELINE, PIPELINE_ID, NEW_PIPELINE_NAME);
  }

  @Test
  public void testCloneEntityUsingYaml_Provisioner() throws Exception {
    doReturn(null).when(provisionerService).getByName(anyString(), anyString());
    doReturn(CloudFormationInfrastructureProvisioner.builder().appId(APP_ID).uuid(PROVISIONER_ID).build())
        .when(provisionerService)
        .get(anyString(), anyString());

    testEntityClone(NEW_PATH_FOR_PROVIOSIONER, EntityType.PROVISIONER, PROVISIONER_ID, NEW_PROVISIONER_NAME);
  }

  private void testEntityClone(String newPathForEntity, EntityType entityType, String entityId, String newEntityName)
      throws Exception {
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        gitFileChangeList = (List<GitFileChange>) args[4];
        gitFileChangeList.add(GitFileChange.Builder.aGitFileChange()
                                  .withFilePath(newPathForEntity)
                                  .withAccountId(ACCOUNT_ID)
                                  .withFileContent("")
                                  .build());
        return StringUtils.EMPTY;
      }
    })
        .when(yamlDirectoryService)
        .getGitFileChange(any(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean());

    doReturn(Collections.EMPTY_LIST).when(yamlService).processChangeSet(any(), anyBoolean());

    yamlCloneService.cloneEntityUsingYaml(ACCOUNT_ID, APP_ID, false, entityType.name(), entityId, newEntityName);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(yamlService).processChangeSet(captor.capture(), anyBoolean());
    List changes = captor.getValue();
    assertNotNull(changes);
    assertEquals(1, changes.size());
    assertTrue(changes.get(0) instanceof Change);
    Change change = (Change) changes.get(0);
    assertEquals(ChangeType.ADD, change.getChangeType());
    assertEquals(ACCOUNT_ID, change.getAccountId());
    assertEquals(newPathForEntity, change.getFilePath());

    verify(yamlDirectoryService, times(1))
        .getGitFileChange(any(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), any(), anyBoolean());

    verify(yamlService, times(1)).processChangeSet(any(), anyBoolean());
  }
}
