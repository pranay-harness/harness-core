package software.wings.yaml.handler.pipeline;

import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.Yaml;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.yaml.handler.workflow.PipelineStageYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.sm.StateType;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author rktummala on 1/9/18
 */
@SetupScheduler
public class PipelineYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock AppService appService;
  @Mock YamlGitService yamlGitService;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock private Account account;
  @Mock private AccountService accountService;
  @Mock private EnvironmentService environmentService;
  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject YamlDirectoryService yamlDirectoryService;
  @InjectMocks @Inject PipelineService pipelineService;
  @InjectMocks @Inject WorkflowService workflowService;
  @InjectMocks @Inject WorkflowServiceHelper workflowServiceHelper;
  @InjectMocks @Inject private PipelineYamlHandler yamlHandler;
  @InjectMocks @Inject private PipelineStageYamlHandler pipelineStageYamlHandler;

  private final String APP_NAME = "app1";
  private final String PIPELINE_NAME = "pipeline1";
  private final String PIPELINE_ID = "pipeline1Id";
  private Pipeline pipeline;

  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: PIPELINE\n"
      + "description: pipeline description\n"
      + "pipelineStages:\n"
      + "- type: APPROVAL\n"
      + "  name: Approval 0\n"
      + "  parallel: false\n"
      + "- type: ENV_STATE\n"
      + "  name: Prod\n"
      + "  parallel: false\n"
      + "  workflowName: WORKFLOW_NAME\n"
      + "  workflowVariables:\n"
      + "  - name: Email\n"
      + "    value: srin@harness.io\n"
      + "  - name: Number\n"
      + "    value: '1234'\n"
      + "  - entityType: ENVIRONMENT\n"
      + "    name: Environment\n"
      + "    value: QA\n"
      + "  - entityType: SERVICE\n"
      + "    name: Service\n"
      + "    value: Todolist\n"
      + "  - entityType: INFRASTRUCTURE_MAPPING\n"
      + "    name: ServiceInfra_SSH\n"
      + "    value: 'Physical Data Center: physical_data_center (Data Center_SSH)'\n"
      + "  - name: MyVar\n"
      + "    value: asasa\n";

  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Pipelines/" + PIPELINE_NAME + ".yaml";
  private String invalidYamlContent = "description1: valid application yaml\ntype: PIPELINE";
  private String invalidYamlFilePath = "Setup/Applications/" + APP_NAME + "/aa/" + PIPELINE_NAME + "/Index.yaml";

  @Before
  public void setUp() throws IOException {
    PipelineStageElement approvalElement =
        PipelineStageElement.builder().name("Approval 0").type(StateType.APPROVAL.name()).build();
    PipelineStage pipelineStage1 =
        PipelineStage.builder().name("stage1").pipelineStageElements(Arrays.asList(approvalElement)).build();
    PipelineStageElement envStateElement =
        PipelineStageElement.builder().name("prod").type(StateType.ENV_STATE.name()).build();
    PipelineStage pipelineStage2 =
        PipelineStage.builder().name("stage2").pipelineStageElements(Arrays.asList(envStateElement)).build();

    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withServiceId(SERVICE_ID)
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withComputeProviderType(AWS.name())
                                                            .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);

    Service service = Service.builder().name(SERVICE_NAME).uuid(SERVICE_ID).artifactType(WAR).build();
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(accountService.get(anyString())).thenReturn(account);

    Application application =
        Application.Builder.anApplication().withName(APP_NAME).withUuid(APP_ID).withAccountId(ACCOUNT_ID).build();
    when(appService.getAppByName(anyString(), anyString())).thenReturn(application);

    when(appService.get(APP_ID)).thenReturn(application);

    Environment environment = anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withAppId(APP_ID).build();
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);

    when(environmentService.getEnvironmentByName(anyString(), anyString(), anyBoolean())).thenReturn(environment);
    when(serviceResourceService.getServiceByName(anyString(), anyString(), anyBoolean())).thenReturn(service);

    when(infrastructureMappingService.getInfraMappingByName(anyString(), anyString(), anyString()))
        .thenReturn(awsInfrastructureMapping);

    when(yamlGitService.get(anyString(), anyString())).thenReturn(null);

    pipeline = Pipeline.builder()
                   .appId(APP_ID)
                   .name(PIPELINE_NAME)
                   .uuid(PIPELINE_ID)
                   .description("pipeline description")
                   .pipelineStages(Arrays.asList(pipelineStage1, pipelineStage2))
                   .build();
    Workflow workflow1 =
        aWorkflow()
            .withEnvId(ENV_ID)
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();
    workflowService.createWorkflow(workflow1);
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PIPELINE);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    Pipeline savedPipeline = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    comparePipeline(pipeline, savedPipeline);

    Yaml yaml = yamlHandler.toYaml(savedPipeline, APP_ID);
    assertNotNull(yaml);
    assertNotNull(yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    //    assertEquals(validYamlContent, yamlContent);

    Pipeline pipelineFromGet = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    comparePipeline(pipeline, pipelineFromGet);

    yamlHandler.delete(changeContext);

    Pipeline pipeline = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(pipeline);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    // Invalid yaml path
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContent);
    gitFileChange.setFilePath(invalidYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException ex) {
      // do nothing
    }

    // Invalid yaml content
    gitFileChange.setFileContent(invalidYamlContent);
    gitFileChange.setFilePath(validYamlFilePath);

    try {
      yamlObject = (Yaml) getYaml(invalidYamlContent, Yaml.class);
      changeContext.setYaml(yamlObject);

      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      failBecauseExceptionWasNotThrown(UnrecognizedPropertyException.class);
    } catch (WingsException ex) {
      // Do nothing
    }
  }

  private void comparePipeline(Pipeline lhs, Pipeline rhs) {
    assertEquals(lhs.getName(), rhs.getName());
    //    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.getDescription(), rhs.getDescription());
  }
}
