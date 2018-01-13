package software.wings.yaml.handler.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.workflow.BuildWorkflowYamlHandler;
import software.wings.yaml.workflow.BuildWorkflowYaml;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/10/18
 */
public class BuildWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String validYamlContent = "envName: ENV_NAME\n"
      + "templatized: false\n"
      + "phases:\n"
      + "  - name: Phase 1\n"
      + "    infraMappingName: direct_Kubernetes\n"
      + "    serviceName: SERVICE_NAME\n"
      + "    provisionNodes: false\n"
      + "    phaseSteps:\n"
      + "      - name: Prepare Steps\n"
      + "        stepsInParallel: false\n"
      + "        type: PREPARE_STEPS\n"
      + "      - name: Collect Artifact\n"
      + "        stepsInParallel: false\n"
      + "        steps:\n"
      + "          - name: Artifact Collection\n"
      + "            properties:\n"
      + "                serviceName: SERVICE_NAME\n"
      + "                buildNo: latest\n"
      + "                artifactStreamName: gcr.io_exploration-161417_todolist\n"
      + "            type: ARTIFACT_COLLECTION\n"
      + "        type: COLLECT_ARTIFACT\n"
      + "      - name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "        type: WRAP_UP\n"
      + "    type: KUBERNETES\n"
      + "notificationRules:\n"
      + "  - conditions:\n"
      + "      - FAILED\n"
      + "    executionScope: WORKFLOW\n"
      + "    notificationGroups:\n"
      + "      - accountId: ACCOUNT_ID\n"
      + "        name: Account Administrator\n"
      + "        editable: false\n"
      + "    batchNotifications: false\n"
      + "    active: true\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: BUILD";
  private String validYamlFilePath = "Setup/Applications/APP_NAME/Workflows/build.yaml";
  private String invalidYamlContent = "envName: env1\nphaseInvalid: phase1\ntype: BUILD";
  private String invalidYamlFilePath = "Setup/Applications/APP_NAME/WorkflowsInvalid/build.yaml";
  private String workflowName = "build";

  @InjectMocks @Inject private BuildWorkflowYamlHandler yamlHandler;

  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, workflowName);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    ChangeContext<BuildWorkflowYaml> changeContext = getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(validYamlContent, BuildWorkflowYaml.class, false);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    assertNotNull(workflow);
    assertEquals(workflow.getName(), workflowName);

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertNotNull(yaml);
    assertEquals(yaml.getType(), "BUILD");

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(savedWorkflow);
    assertEquals(savedWorkflow.getName(), workflowName);

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(deletedWorkflow);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    testFailures(validYamlContent, validYamlFilePath, invalidYamlContent, invalidYamlFilePath, yamlHandler,
        BuildWorkflowYaml.class);
  }
}
