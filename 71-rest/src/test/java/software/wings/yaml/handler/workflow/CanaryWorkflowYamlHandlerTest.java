package software.wings.yaml.handler.workflow;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_INVALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_INVALID_YAML_FILE_PATH;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_VALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_VALID_YAML_FILE_PATH;

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
import software.wings.service.impl.yaml.handler.workflow.CanaryWorkflowYamlHandler;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

import java.io.IOException;

/**
 * @author rktummala on 1/10/18
 */
public class CanaryWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String workflowName = "canary";

  @InjectMocks @Inject private CanaryWorkflowYamlHandler yamlHandler;
  @Mock private LimitCheckerFactory limitCheckerFactory;

  @Before
  public void runBeforeTest() {
    setup(CANARY_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    ChangeContext<CanaryWorkflowYaml> changeContext =
        getChangeContext(CANARY_VALID_YAML_CONTENT, CANARY_VALID_YAML_FILE_PATH, yamlHandler);

    CanaryWorkflowYaml yamlObject = (CanaryWorkflowYaml) getYaml(CANARY_VALID_YAML_CONTENT, CanaryWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(workflow);
    assertEquals(workflow.getName(), workflowName);

    CanaryWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertNotNull(yaml);
    assertEquals("CANARY", yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(CANARY_VALID_YAML_CONTENT, yamlContent);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(savedWorkflow);
    assertEquals(savedWorkflow.getName(), workflowName);

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, CANARY_VALID_YAML_FILE_PATH);
    assertNull(deletedWorkflow);
  }

  @Test
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    testFailures(CANARY_VALID_YAML_CONTENT, CANARY_VALID_YAML_FILE_PATH, CANARY_INVALID_YAML_CONTENT,
        CANARY_INVALID_YAML_FILE_PATH, yamlHandler, CanaryWorkflowYaml.class);
  }
}
