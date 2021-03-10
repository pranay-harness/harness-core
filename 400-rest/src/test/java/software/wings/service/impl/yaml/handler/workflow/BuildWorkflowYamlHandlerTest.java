package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.DHRUV;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.RAMA;

import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_INVALID_YAML_CONTENT_RESOURCE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_INVALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_CONTENT_INFRA_DEF_RESOURCE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_CONTENT_RESOURCE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_USER_GROUP_RESOURCE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_USER_GROUP_TEMPLATIZED2_RESOURCE_PATH;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;

import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.workflow.BuildWorkflowYaml;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author rktummala on 1/10/18
 */
public class BuildWorkflowYamlHandlerTest extends WorkflowYamlHandlerTestBase {
  private String workflowName = "build";

  @InjectMocks @Inject private BuildWorkflowYamlHandler yamlHandler;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private FeatureFlagService featureFlagService;
  @Before
  public void runBeforeTest() {
    setup(BUILD_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    String yamlContentFromFile = readYamlStringInFile(BUILD_VALID_YAML_CONTENT_RESOURCE_PATH);
    ChangeContext<BuildWorkflowYaml> changeContext =
        getChangeContext(yamlContentFromFile, BUILD_VALID_YAML_FILE_PATH, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(yamlContentFromFile, BuildWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("BUILD");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlContent);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(savedWorkflow).isNotNull();
    assertThat(workflowName).isEqualTo(savedWorkflow.getName());

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BUILD_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testCRUDAndGetInfrDef() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    String yamlStringInFile = readYamlStringInFile(BUILD_VALID_YAML_CONTENT_INFRA_DEF_RESOURCE_PATH);
    ChangeContext<BuildWorkflowYaml> changeContext =
        getChangeContext(yamlStringInFile, BUILD_VALID_YAML_FILE_PATH, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(yamlStringInFile, BuildWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("BUILD");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlStringInFile);

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BUILD_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testFailures() throws Exception {
    testFailures(readYamlStringInFile(BUILD_VALID_YAML_CONTENT_RESOURCE_PATH), BUILD_VALID_YAML_FILE_PATH,
        BUILD_INVALID_YAML_CONTENT_RESOURCE_PATH, BUILD_INVALID_YAML_FILE_PATH, yamlHandler, BuildWorkflowYaml.class);
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDForUserGroupTemplatized() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    String yamlFileContent = readYamlStringInFile(BUILD_VALID_YAML_USER_GROUP_TEMPLATIZED2_RESOURCE_PATH);
    ChangeContext<BuildWorkflowYaml> changeContext =
        getChangeContext(yamlFileContent, BUILD_VALID_YAML_FILE_PATH, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(yamlFileContent, BuildWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("BUILD");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlFileContent);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    assertThat(savedWorkflow).isNotNull();
    assertThat(workflowName).isEqualTo(savedWorkflow.getName());

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BUILD_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDForUserGroup() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    String yamlStringInFile = readYamlStringInFile(BUILD_VALID_YAML_USER_GROUP_RESOURCE_PATH);
    ChangeContext<BuildWorkflowYaml> changeContext =
        getChangeContext(yamlStringInFile, BUILD_VALID_YAML_FILE_PATH, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(yamlStringInFile, BuildWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("BUILD");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlStringInFile);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    assertThat(savedWorkflow).isNotNull();
    assertThat(workflowName).isEqualTo(savedWorkflow.getName());

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BUILD_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }
}
