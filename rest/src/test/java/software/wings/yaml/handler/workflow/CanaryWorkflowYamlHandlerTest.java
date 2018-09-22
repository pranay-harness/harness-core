package software.wings.yaml.handler.workflow;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_INVALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_INVALID_YAML_FILE_PATH;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_VALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.CANARY_VALID_YAML_FILE_PATH;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.workflow.CanaryWorkflowYamlHandler;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

import java.io.IOException;

/**
 * @author rktummala on 1/10/18
 */
public class CanaryWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String workflowName = "canary";

  @InjectMocks @Inject private CanaryWorkflowYamlHandler yamlHandler;

  @Before
  public void runBeforeTest() {
    setup(CANARY_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
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
  public void testFailures() throws HarnessException, IOException {
    testFailures(CANARY_VALID_YAML_CONTENT, CANARY_VALID_YAML_FILE_PATH, CANARY_INVALID_YAML_CONTENT,
        CANARY_INVALID_YAML_FILE_PATH, yamlHandler, CanaryWorkflowYaml.class);
  }
}
