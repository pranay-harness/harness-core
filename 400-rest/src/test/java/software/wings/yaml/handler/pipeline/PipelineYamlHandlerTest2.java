package software.wings.yaml.handler.pipeline;

import static io.harness.rule.OwnerRule.DHRUV;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;

import software.wings.api.CloudProviderType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.Yaml;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.yaml.WorkflowYAMLHelper;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.sm.StateType;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.handler.YamlHandlerTestBase;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author rktummala on 1/9/18
 */
@SetupScheduler
public class PipelineYamlHandlerTest2 extends YamlHandlerTestBase {
  private final String APP_NAME = "app1";
  private final String PIPELINE_NAME = "pipeline1";
  private final String PIPELINE_ID = "pipeline1Id";
  private final String resourcePath = "400-rest/src/test/resources/pipeline";
  @Mock AppService appService;
  @Mock YamlGitService yamlGitService;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock ServiceResourceService serviceResourceService;
  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject YamlDirectoryService yamlDirectoryService;
  @InjectMocks @Inject WorkflowServiceHelper workflowServiceHelper;
  @Mock private Account account;
  @Mock private AccountService accountService;
  @Mock private EnvironmentService environmentService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private UserGroupService userGroupService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;
  @Mock private WorkflowYAMLHelper workflowYAMLHelper;
  @InjectMocks @Inject private PipelineYamlHandler yamlHandler;

  private String validYamlFilePath = "Setup/Applications/" + APP_NAME + "/Pipelines/" + PIPELINE_NAME + ".yaml";

  @Before
  public void setUp() throws IOException {
    InfrastructureDefinition awsInfraDefinition = InfrastructureDefinition.builder()
                                                      .uuid(INFRA_DEFINITION_ID)
                                                      .deploymentType(SSH)
                                                      .cloudProviderType(CloudProviderType.AWS)
                                                      .infrastructure(AwsInstanceInfrastructure.builder().build())
                                                      .build();
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(awsInfraDefinition);

    UserGroup userGroup = new UserGroup();
    userGroup.setName("Account Administrator");
    userGroup.setUuid("dIyaCXXVRp65abGOlN5Fmg");
    UserGroup userGroup2 = new UserGroup();
    userGroup2.setName("Non-Production Support");
    userGroup2.setUuid("s6dYbwVXQ1-Bgq234fbznw");
    when(userGroupService.fetchUserGroupByName(any(), eq("Account Administrator"))).thenReturn(userGroup);
    when(userGroupService.fetchUserGroupByName(any(), eq("test")))
        .thenReturn(UserGroup.builder().name("test").uuid("test").build());
    when(userGroupService.fetchUserGroupByName(any(), eq("Non-Production Support"))).thenReturn(userGroup2);
    when(userGroupService.get(any(), eq("test"))).thenReturn(UserGroup.builder().name("test").uuid("test").build());
    when(userGroupService.get(any(), eq("dIyaCXXVRp65abGOlN5Fmg"))).thenReturn(userGroup);
    when(userGroupService.get(any(), eq("s6dYbwVXQ1-Bgq234fbznw"))).thenReturn(userGroup2);

    Service service = Service.builder().name(SERVICE_NAME).uuid(SERVICE_ID).artifactType(WAR).build();
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(service);
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(service);
    when(accountService.get(anyString())).thenReturn(account);
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    Application application =
        Application.Builder.anApplication().name(APP_NAME).uuid(APP_ID).accountId(ACCOUNT_ID).build();
    when(appService.getAppByName(anyString(), anyString())).thenReturn(application);

    when(appService.get(APP_ID)).thenReturn(application);

    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).appId(APP_ID).build();
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);

    when(environmentService.getEnvironmentByName(anyString(), anyString(), anyBoolean())).thenReturn(environment);
    when(serviceResourceService.getServiceByName(anyString(), anyString(), anyBoolean())).thenReturn(service);

    when(yamlGitService.get(anyString(), anyString(), any())).thenReturn(null);

    Workflow workflow1 = aWorkflow()
                             .envId(ENV_ID)
                             .name(WORKFLOW_NAME)
                             .appId(APP_ID)
                             .workflowType(WorkflowType.ORCHESTRATION)
                             .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                                        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                                        .addWorkflowPhase(aWorkflowPhase()
                                                                              .infraDefinitionId(INFRA_DEFINITION_ID)
                                                                              .serviceId(SERVICE_ID)
                                                                              .deploymentType(SSH)
                                                                              .build())
                                                        .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                                        .build())
                             .build();
    workflowService.createWorkflow(workflow1);

    when(workflowYAMLHelper.getWorkflowVariableValueBean(
             anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(4, String.class));
    when(workflowYAMLHelper.getWorkflowVariableValueYaml(anyString(), anyString(), any(), anyBoolean()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(1, String.class));
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDAndGetUserGroup() throws Exception {
    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineUserGroup));
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDAndGetUserGroupTemplatized() throws Exception {
    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineUserGroupTemplatized));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDPipelineTemplatized() throws Exception {
    List<Variable> variables = Arrays.asList(aVariable().name("Environment").entityType(EntityType.ENVIRONMENT).build(),
        aVariable().name("Service").entityType(EntityType.SERVICE).build(),
        aVariable().name("InfraDefinition_KUBERNETES").entityType(EntityType.INFRASTRUCTURE_DEFINITION).build());

    Workflow workflow = aWorkflow()
                            .name("K8s-roll")
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(variables).build())
                            .build();
    when(workflowService.readWorkflowByName(any(), any())).thenReturn(workflow);
    when(workflowService.readWorkflow(any(), any())).thenReturn(workflow);

    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineTemplatized));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDPipelineRuntime() throws Exception {
    List<Variable> variables = Arrays.asList(aVariable().name("Environment").entityType(EntityType.ENVIRONMENT).build(),
        aVariable().name("Service").entityType(EntityType.SERVICE).build(),
        aVariable().name("InfraDefinition_KUBERNETES").entityType(EntityType.INFRASTRUCTURE_DEFINITION).build());

    Workflow workflow = aWorkflow()
                            .name("K8s-roll")
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(variables).build())
                            .build();
    when(workflowService.readWorkflowByName(any(), any())).thenReturn(workflow);
    when(workflowService.readWorkflow(any(), any())).thenReturn(workflow);

    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineRuntime));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDPipelineNewRelic() throws Exception {
    List<Variable> variables =
        Arrays.asList(aVariable().name("NewRelic_Server").entityType(EntityType.NEWRELIC_CONFIGID).build(),
            aVariable().name("NewRelic_Application").entityType(EntityType.NEWRELIC_APPID).build());

    Workflow workflow = aWorkflow()
                            .name("New-relic")
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(variables).build())
                            .build();
    when(workflowService.readWorkflowByName(any(), any())).thenReturn(workflow);
    when(workflowService.readWorkflow(any(), any())).thenReturn(workflow);

    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineNewRelic));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDPipelineThreeStages() throws Exception {
    List<Variable> variables =
        Arrays.asList(aVariable().name("NewRelic_Server").entityType(EntityType.NEWRELIC_CONFIGID).build(),
            aVariable().name("NewRelic_Application").entityType(EntityType.NEWRELIC_APPID).build());

    Workflow workflow = aWorkflow()
                            .name("New-relic")
                            .uuid("New-relic")
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(variables).build())
                            .build();
    when(workflowService.readWorkflowByName(any(), eq("New-relic"))).thenReturn(workflow);
    when(workflowService.readWorkflow(any(), eq("New-relic"))).thenReturn(workflow);
    List<Variable> variables2 = Arrays.asList(aVariable().name("wvar").type(VariableType.TEXT).build(),
        aVariable().name("wvar2").type(VariableType.TEXT).build());
    Workflow workflow2 =
        aWorkflow()
            .name("Build")
            .uuid("Build")
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(variables2).build())
            .build();
    when(workflowService.readWorkflowByName(any(), eq("Build"))).thenReturn(workflow2);
    when(workflowService.readWorkflow(any(), eq("Build"))).thenReturn(workflow2);

    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineThreeStages));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDSnowApproval() throws Exception {
    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineSnowApproval));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCRUDApprovalVariable() throws Exception {
    testCRUDpipeline(readYamlStringInFile(ValidPipelineFiles.pipelineApprovalVariable));
  }

  private void testCRUDpipeline(String validYamlContent) throws IOException {
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

    when(pipelineService.save(any())).thenAnswer(invocationOnMock -> {
      Pipeline pipeline = invocationOnMock.getArgumentAt(0, Pipeline.class);
      pipeline.getPipelineStages()
          .stream()
          .flatMap(ps -> ps.getPipelineStageElements().stream())
          .filter(pse -> pse.getType().equals(StateType.ENV_STATE.name()))
          .forEach(pse -> pse.getProperties().put("workflowId", pse.getName()));
      return pipeline;
    });
    Pipeline savedPipeline = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));

    Yaml yaml = yamlHandler.toYaml(savedPipeline, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isNotNull();

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(validYamlContent);

    yamlHandler.delete(changeContext);

    Pipeline pipeline = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(pipeline).isNull();
  }

  protected String readYamlStringInFile(String yamlContentResourcePath) throws IOException {
    File yamlFile = new File(resourcePath + PATH_DELIMITER + yamlContentResourcePath);
    assertThat(yamlFile).isNotNull();
    return FileUtils.readFileToString(yamlFile, "UTF-8");
  }

  @UtilityClass
  private static class ValidPipelineFiles {
    private static final String pipelineTemplatized = "pipelineTemplatized.yaml";
    private static final String pipelineRuntime = "pipelineRuntimeVars.yaml";
    private static final String pipelineNewRelic = "pipelineNewRelic.yaml";
    private static final String pipelineThreeStages = "pipelineThreeStages.yaml";
    private static final String pipelineSnowApproval = "pipelineSnowApproval.yaml";
    private static final String pipelineApprovalVariable = "pipelineApprovalVariable.yaml";
    private static final String pipelineUserGroup = "pipelineUserGroup.yaml";
    private static final String pipelineUserGroupTemplatized = "pipelineUserGroupTemplatized.yaml";
  }
}
