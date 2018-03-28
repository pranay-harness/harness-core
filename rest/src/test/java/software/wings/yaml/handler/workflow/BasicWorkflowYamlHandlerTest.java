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
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.yaml.workflow.BasicWorkflowYaml;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/9/18
 */
public class BasicWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: BASIC\n"
      + "envName: ENV_NAME\n"
      + "failureStrategies:\n"
      + "  - executionScope: WORKFLOW\n"
      + "    failureTypes:\n"
      + "      - APPLICATION_ERROR\n"
      + "    repairActionCode: ROLLBACK_WORKFLOW\n"
      + "    retryCount: 0\n"
      + "notificationRules:\n"
      + "  - conditions:\n"
      + "      - FAILED\n"
      + "    executionScope: WORKFLOW\n"
      + "    notificationGroups:\n"
      + "      - Account Administrator\n"
      + "phases:\n"
      + "  - type: KUBERNETES\n"
      + "    computeProviderName: exploration\n"
      + "    daemonSet: false\n"
      + "    infraMappingName: direct_Kubernetes\n"
      + "    name: Phase 1\n"
      + "    phaseSteps:\n"
      + "      - type: CONTAINER_SETUP\n"
      + "        name: Setup Container\n"
      + "        steps:\n"
      + "          - type: KUBERNETES_SETUP\n"
      + "            name: Kubernetes Service Setup\n"
      + "        stepsInParallel: false\n"
      + "      - type: CONTAINER_DEPLOY\n"
      + "        name: Deploy Containers\n"
      + "        steps:\n"
      + "          - type: KUBERNETES_DEPLOY\n"
      + "            name: Upgrade Containers\n"
      + "            properties:\n"
      + "                commandName: Resize Replication Controller\n"
      + "                instanceUnitType: COUNT\n"
      + "                instanceCount: 1\n"
      + "        stepsInParallel: false\n"
      + "      - type: VERIFY_SERVICE\n"
      + "        name: Verify Service\n"
      + "        stepsInParallel: false\n"
      + "      - type: WRAP_UP\n"
      + "        name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "    provisionNodes: false\n"
      + "    serviceName: SERVICE_NAME\n"
      + "rollbackPhases:\n"
      + "  - type: KUBERNETES\n"
      + "    computeProviderName: exploration\n"
      + "    daemonSet: false\n"
      + "    infraMappingName: direct_Kubernetes\n"
      + "    name: Rollback Phase 1\n"
      + "    phaseNameForRollback: Phase 1\n"
      + "    phaseSteps:\n"
      + "      - type: CONTAINER_DEPLOY\n"
      + "        name: Deploy Containers\n"
      + "        phaseStepNameForRollback: Deploy Containers\n"
      + "        statusForRollback: SUCCESS\n"
      + "        steps:\n"
      + "          - type: KUBERNETES_DEPLOY_ROLLBACK\n"
      + "            name: Rollback Containers\n"
      + "        stepsInParallel: false\n"
      + "      - type: WRAP_UP\n"
      + "        name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "    provisionNodes: false\n"
      + "    serviceName: SERVICE_NAME\n"
      + "templatized: false";

  private String validYamlContentTemplatized = "harnessApiVersion: '1.0'\n"
      + "type: BASIC\n"
      + "envName: Test Environment\n"
      + "failureStrategies:\n"
      + "  - executionScope: WORKFLOW\n"
      + "    failureTypes:\n"
      + "      - APPLICATION_ERROR\n"
      + "    repairActionCode: ROLLBACK_WORKFLOW\n"
      + "    retryCount: 0\n"
      + "notificationRules:\n"
      + "  - conditions:\n"
      + "      - FAILED\n"
      + "    executionScope: WORKFLOW\n"
      + "    notificationGroups:\n"
      + "      - Account Administrator\n"
      + "phases:\n"
      + "  - computeProviderName: Aws non-prod\n"
      + "    daemonSet: false\n"
      + "    name: Phase 1\n"
      + "    phaseSteps:\n"
      + "      - type: CONTAINER_SETUP\n"
      + "        name: Setup Container\n"
      + "        steps:\n"
      + "          - type: ECS_SERVICE_SETUP\n"
      + "            name: ECS Service Setup\n"
      + "        stepsInParallel: false\n"
      + "      - type: CONTAINER_DEPLOY\n"
      + "        name: Deploy Containers\n"
      + "        steps:\n"
      + "          - type: ECS_SERVICE_DEPLOY\n"
      + "            name: Upgrade Containers\n"
      + "        stepsInParallel: false\n"
      + "      - type: VERIFY_SERVICE\n"
      + "        name: Verify Service\n"
      + "        stepsInParallel: false\n"
      + "      - type: WRAP_UP\n"
      + "        name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "    provisionNodes: false\n"
      + "    serviceName: Test\n"
      + "    templateExpressions:\n"
      + "      - expression: ${Service}\n"
      + "        fieldName: serviceId\n"
      + "        metadata:\n"
      + "          - name: artifactType\n"
      + "            value: DOCKER\n"
      + "          - name: relatedField\n"
      + "            value: ${ServiceInfra_ECS}\n"
      + "          - name: entityType\n"
      + "            value: SERVICE\n"
      + "      - expression: ${ServiceInfra_ECS}\n"
      + "        fieldName: infraMappingId\n"
      + "        metadata:\n"
      + "          - name: artifactType\n"
      + "            value: DOCKER\n"
      + "          - name: relatedField\n"
      + "          - name: entityType\n"
      + "            value: INFRASTRUCTURE_MAPPING\n"
      + "rollbackPhases:\n"
      + "  - computeProviderName: Aws non-prod\n"
      + "    daemonSet: false\n"
      + "    name: Rollback Phase 1\n"
      + "    phaseNameForRollback: Phase 1\n"
      + "    phaseSteps:\n"
      + "      - type: CONTAINER_DEPLOY\n"
      + "        name: Deploy Containers\n"
      + "        phaseStepNameForRollback: Deploy Containers\n"
      + "        statusForRollback: SUCCESS\n"
      + "        steps:\n"
      + "          - type: ECS_SERVICE_ROLLBACK\n"
      + "            name: Rollback Containers\n"
      + "            properties:\n"
      + "                rollback: true\n"
      + "        stepsInParallel: false\n"
      + "      - type: VERIFY_SERVICE\n"
      + "        name: Verify Service\n"
      + "        phaseStepNameForRollback: Deploy Containers\n"
      + "        statusForRollback: SUCCESS\n"
      + "        stepsInParallel: false\n"
      + "      - type: WRAP_UP\n"
      + "        name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "    provisionNodes: false\n"
      + "    serviceName: Test\n"
      + "    templateExpressions:\n"
      + "      - expression: ${Service}\n"
      + "        fieldName: serviceId\n"
      + "        metadata:\n"
      + "          - name: artifactType\n"
      + "            value: DOCKER\n"
      + "          - name: relatedField\n"
      + "            value: ${ServiceInfra_ECS}\n"
      + "          - name: entityType\n"
      + "            value: SERVICE\n"
      + "      - expression: ${ServiceInfra_ECS}\n"
      + "        fieldName: infraMappingId\n"
      + "        metadata:\n"
      + "          - name: artifactType\n"
      + "            value: DOCKER\n"
      + "          - name: relatedField\n"
      + "          - name: entityType\n"
      + "            value: INFRASTRUCTURE_MAPPING\n"
      + "templateExpressions:\n"
      + "  - expression: ${Service}\n"
      + "    fieldName: serviceId\n"
      + "    metadata:\n"
      + "      - name: artifactType\n"
      + "        value: DOCKER\n"
      + "      - name: relatedField\n"
      + "        value: ${ServiceInfra_ECS}\n"
      + "      - name: entityType\n"
      + "        value: SERVICE\n"
      + "  - expression: ${ServiceInfra_ECS}\n"
      + "    fieldName: infraMappingId\n"
      + "    metadata:\n"
      + "      - name: artifactType\n"
      + "        value: DOCKER\n"
      + "      - name: relatedField\n"
      + "      - name: entityType\n"
      + "        value: INFRASTRUCTURE_MAPPING\n"
      + "templatized: true\n"
      + "userVariables:\n"
      + "  - type: ENTITY\n"
      + "    description: Variable for Service entity\n"
      + "    fixed: false\n"
      + "    mandatory: true\n"
      + "    name: Service\n"
      + "  - type: ENTITY\n"
      + "    description: Variable for Service Infra-structure entity\n"
      + "    fixed: false\n"
      + "    mandatory: true\n"
      + "    name: ServiceInfra_ECS\n";

  private String validYamlFilePath = "Setup/Applications/APP_NAME/Workflows/basic.yaml";
  private String invalidYamlContent = "envName: env1\nphaseInvalid: phase1\ntype: BASIC";
  private String invalidYamlFilePath = "Setup/Applications/APP_NAME/WorkflowsInvalid/basic.yaml";
  private String workflowName = "basic";

  @InjectMocks @Inject private BasicWorkflowYamlHandler yamlHandler;
  @Before
  public void runBeforeTest() {
    setup(validYamlFilePath, workflowName);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    testCRUD(validYamlContent);
    testCRUD(validYamlContentTemplatized);
  }

  private void testCRUD(String yamlString) throws IOException, HarnessException {
    ChangeContext<BasicWorkflowYaml> changeContext = getChangeContext(yamlString, validYamlFilePath, yamlHandler);

    BasicWorkflowYaml yamlObject = (BasicWorkflowYaml) getYaml(yamlString, BasicWorkflowYaml.class, false);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    assertNotNull(workflow);
    assertEquals(workflow.getName(), workflowName);

    BasicWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertNotNull(yaml);
    assertEquals("BASIC", yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);

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
        BasicWorkflowYaml.class);
  }
}
