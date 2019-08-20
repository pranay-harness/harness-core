package software.wings.yaml.handler.workflow;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_INVALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_INVALID_YAML_FILE_PATH;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_VALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_VALID_YAML_CONTENT_TEMPLATIZED;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_VALID_YAML_CONTENT_WITH_MULTILINE_USER_INPUT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BASIC_VALID_YAML_FILE_PATH;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.workflow.BasicWorkflowYaml;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/9/18
 */
public class BasicWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String workflowName = "basic";

  @Mock private LimitCheckerFactory limitCheckerFactory;

  @InjectMocks @Inject private BasicWorkflowYamlHandler yamlHandler;

  @Before
  public void runBeforeTest() {
    setup(BASIC_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    testCRUD(BASIC_VALID_YAML_CONTENT);
    testCRUD(BASIC_VALID_YAML_CONTENT_TEMPLATIZED);
    testCRUDWithYamlWithMultilineUserInput();
  }

  private void testCRUDWithYamlWithMultilineUserInput() throws IOException, HarnessException {
    String yamlString = BASIC_VALID_YAML_CONTENT_WITH_MULTILINE_USER_INPUT;

    for (int count = 0; count < 3; count++) {
      ChangeContext<BasicWorkflowYaml> changeContext =
          getChangeContext(yamlString, BASIC_VALID_YAML_FILE_PATH, yamlHandler);

      BasicWorkflowYaml yamlObject = (BasicWorkflowYaml) getYaml(yamlString, BasicWorkflowYaml.class);
      changeContext.setYaml(yamlObject);

      Workflow workflow = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertNotNull(workflow);
      assertEquals(workflow.getName(), workflowName);

      BasicWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
      assertNotNull(yaml);
      assertEquals("BASIC", yaml.getType());

      String yamlContent = getYamlContent(yaml);
      yamlString = yamlContent.substring(0, yamlContent.length() - 1);
      assertEquals(BASIC_VALID_YAML_CONTENT_WITH_MULTILINE_USER_INPUT, yamlString);
    }
  }

  private void testCRUD(String yamlString) throws IOException, HarnessException {
    ChangeContext<BasicWorkflowYaml> changeContext =
        getChangeContext(yamlString, BASIC_VALID_YAML_FILE_PATH, yamlHandler);

    BasicWorkflowYaml yamlObject = (BasicWorkflowYaml) getYaml(yamlString, BasicWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(workflow);
    assertEquals(workflow.getName(), workflowName);

    BasicWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertNotNull(yaml);
    assertEquals("BASIC", yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(yamlString, yamlContent);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(savedWorkflow);
    assertEquals(savedWorkflow.getName(), workflowName);

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BASIC_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    testFailures(BASIC_VALID_YAML_CONTENT, BASIC_VALID_YAML_FILE_PATH, BASIC_INVALID_YAML_CONTENT,
        BASIC_INVALID_YAML_FILE_PATH, yamlHandler, BasicWorkflowYaml.class);
  }
}
