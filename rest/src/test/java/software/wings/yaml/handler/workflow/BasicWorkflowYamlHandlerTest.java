package software.wings.yaml.handler.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ObjectType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.notification.NotificationGroupYamlHandler;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.FailureStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PhaseStepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowPhaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.handler.BaseYamlHandlerTest;
import software.wings.yaml.workflow.BasicWorkflowYaml;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/9/18
 */
public class BasicWorkflowYamlHandlerTest extends BaseYamlHandlerTest {
  private String validYamlContent = "envName: ENV_NAME\n"
      + "templatized: false\n"
      + "phases:\n"
      + "  - name: Phase 1\n"
      + "    infraMappingName: direct_Kubernetes\n"
      + "    serviceName: SERVICE_NAME\n"
      + "    computeProviderName: exploration\n"
      + "    provisionNodes: false\n"
      + "    phaseSteps:\n"
      + "      - name: Setup Container\n"
      + "        stepsInParallel: false\n"
      + "        steps:\n"
      + "          - name: Kubernetes Service Setup\n"
      + "            rollback: false\n"
      + "            type: KUBERNETES_REPLICATION_CONTROLLER_SETUP\n"
      + "        rollback: false\n"
      + "        type: CONTAINER_SETUP\n"
      + "      - name: Deploy Containers\n"
      + "        stepsInParallel: false\n"
      + "        steps:\n"
      + "          - name: Upgrade Containers\n"
      + "            rollback: false\n"
      + "            properties:\n"
      + "                commandName: Resize Replication Controller\n"
      + "                instanceUnitType: COUNT\n"
      + "                instanceCount: 1\n"
      + "            type: KUBERNETES_REPLICATION_CONTROLLER_DEPLOY\n"
      + "        rollback: false\n"
      + "        type: CONTAINER_DEPLOY\n"
      + "      - name: Verify Service\n"
      + "        stepsInParallel: false\n"
      + "        rollback: false\n"
      + "        type: VERIFY_SERVICE\n"
      + "      - name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "        rollback: false\n"
      + "        type: WRAP_UP\n"
      + "    type: KUBERNETES\n"
      + "rollbackPhases:\n"
      + "  - name: Rollback Phase 1\n"
      + "    infraMappingName: direct_Kubernetes\n"
      + "    serviceName: SERVICE_NAME\n"
      + "    computeProviderName: exploration\n"
      + "    provisionNodes: false\n"
      + "    phaseNameForRollback: Phase 1\n"
      + "    phaseSteps:\n"
      + "      - name: Deploy Containers\n"
      + "        statusForRollback: SUCCESS\n"
      + "        stepsInParallel: false\n"
      + "        steps:\n"
      + "          - name: Rollback Containers\n"
      + "            rollback: false\n"
      + "            properties:\n"
      + "                rollback: true\n"
      + "            type: KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK\n"
      + "        rollback: true\n"
      + "        phaseStepNameForRollback: Deploy Containers\n"
      + "        type: CONTAINER_DEPLOY\n"
      + "      - name: Wrap Up\n"
      + "        stepsInParallel: false\n"
      + "        rollback: false\n"
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
      + "failureStrategies:\n"
      + "  - failureTypes:\n"
      + "      - APPLICATION_ERROR\n"
      + "    executionScope: WORKFLOW\n"
      + "    repairActionCode: ROLLBACK_WORKFLOW\n"
      + "    retryCount: 0\n"
      + "type: BASIC";
  private String validYamlFilePath = "Setup/Applications/APP_NAME/Workflows/basic.yaml";
  private String invalidYamlContent = "envName: env1\nphaseInvalid: phase1\ntype: BASIC";
  private String invalidYamlFilePath = "Setup/Applications/APP_NAME/WorkflowsInvalid/basic.yaml";
  private String workflowName = "basic";

  private InfrastructureMapping infrastructureMapping = getInfraMapping();
  private Service service = getService();
  private Environment environment = getEnvironment();

  @Mock YamlHelper yamlHelper;
  @Mock YamlHandlerFactory yamlHandlerFactory;
  @Mock EnvironmentService environmentService;

  @Mock private AppService appService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ServiceResourceService serviceResourceService;

  //  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject WorkflowService workflowService;
  @InjectMocks @Inject WorkflowPhaseYamlHandler phaseYamlHandler;
  @InjectMocks @Inject PhaseStepYamlHandler phaseStepYamlHandler;
  @InjectMocks @Inject StepYamlHandler stepYamlHandler;
  @InjectMocks @Inject FailureStrategyYamlHandler failureStrategyYamlHandler;
  @InjectMocks @Inject NotificationRulesYamlHandler notificationRulesYamlHandler;
  @InjectMocks @Inject NotificationGroupYamlHandler notificationGroupYamlHandler;
  @InjectMocks @Inject TemplateExpressionYamlHandler templateExpressionYamlHandler;
  @InjectMocks @Inject private BasicWorkflowYamlHandler yamlHandler;

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    GitFileChange gitFileChange = spy(GitFileChange.class);
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<BasicWorkflowYaml> changeContext = spy(ChangeContext.class);
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.WORKFLOW);
    changeContext.setYamlSyncHandler(yamlHandler);

    BasicWorkflowYaml yamlObject = (BasicWorkflowYaml) getYaml(validYamlContent, BasicWorkflowYaml.class, false);
    changeContext.setYaml(yamlObject);

    when(appService.getAppByName(anyString(), anyString()))
        .thenReturn(Application.Builder.anApplication().withName(APP_NAME).withUuid(APP_ID).build());

    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(APP_ID);
    when(yamlHelper.getNameFromYamlFilePath(validYamlFilePath)).thenReturn(workflowName);
    when(yamlHelper.extractEntityNameFromYamlPath(
             YamlType.WORKFLOW.getPathExpression(), validYamlFilePath, PATH_DELIMITER))
        .thenReturn(workflowName);

    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(environment);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);

    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);

    when(infrastructureMappingService.getInfraMappingByName(anyString(), anyString(), anyString()))
        .thenReturn(infrastructureMapping);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    when(yamlHandlerFactory.getYamlHandler(YamlType.PHASE, ObjectType.PHASE)).thenReturn(phaseYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP, ObjectType.PHASE_STEP))
        .thenReturn(phaseStepYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION))
        .thenReturn(templateExpressionYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP)).thenReturn(stepYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY))
        .thenReturn(failureStrategyYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE, ObjectType.NOTIFICATION_RULE))
        .thenReturn(notificationRulesYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_GROUP, ObjectType.NOTIFICATION_GROUP))
        .thenReturn(notificationGroupYamlHandler);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    assertNotNull(workflow);
    assertEquals(workflow.getName(), workflowName);

    BasicWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertNotNull(yaml);
    assertEquals(yaml.getType(), "BASIC");

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
    // Invalid yaml path
    GitFileChange gitFileChange = spy(GitFileChange.class);
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(invalidYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<BasicWorkflowYaml> changeContext = spy(ChangeContext.class);
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.WORKFLOW);
    changeContext.setYamlSyncHandler(yamlHandler);

    BasicWorkflowYaml yamlObject = (BasicWorkflowYaml) getYaml(validYamlContent, BasicWorkflowYaml.class, false);
    changeContext.setYaml(yamlObject);

    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (WingsException ex) {
    }

    // Invalid yaml content
    gitFileChange.setFileContent(invalidYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);

    try {
      yamlObject = (BasicWorkflowYaml) getYaml(invalidYamlContent, BasicWorkflowYaml.class, false);
      changeContext.setYaml(yamlObject);

      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
  }

  private InfrastructureMapping getInfraMapping() {
    return GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
        .withName("direct_Kubernetes")
        .withAppId(APP_ID)
        .withClusterName("testCluster")
        .withServiceId(SERVICE_ID)
        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
        .withEnvId(ENV_ID)
        .withComputeProviderType("DIRECT")
        .withUuid(INFRA_MAPPING_ID)
        .withDeploymentType(DeploymentType.KUBERNETES.name())
        .build();
  }

  private Service getService() {
    return Service.Builder.aService()
        .withName(SERVICE_NAME)
        .withAppId(APP_ID)
        .withUuid(SERVICE_ID)
        .withArtifactType(ArtifactType.DOCKER)
        .build();
  }

  private Environment getEnvironment() {
    return Environment.Builder.anEnvironment().withName(ENV_NAME).withAppId(APP_ID).withUuid(ENV_ID).build();
  }
}
