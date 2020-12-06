package software.wings.yaml.handler.pipeline;

import static io.harness.rule.OwnerRule.DHRUV;

import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
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
import static org.mockito.Mockito.doReturn;
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
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.Yaml;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.SSHKeyDataProvider;
import software.wings.service.impl.WinRmConnectionAttributesDataProvider;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.yaml.WorkflowYAMLHelper;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.handler.workflow.PipelineStageYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PipelineYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EntityVersionService;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class PipelineYamlHandlerUserGroupTest extends YamlHandlerTestBase {
  @Mock AppService appService;
  @Mock YamlGitService yamlGitService;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock private Account account;
  @Mock private AccountService accountService;
  @Mock private EnvironmentService environmentService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private UserGroupService userGroupService;
  @Mock private PipelineService pipelineService;

  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject WorkflowYAMLHelper workflowYAMLHelper;
  @InjectMocks @Inject YamlDirectoryService yamlDirectoryService;
  @InjectMocks @Inject WorkflowService workflowService;
  @InjectMocks @Inject WorkflowServiceHelper workflowServiceHelper;
  @InjectMocks @Inject private PipelineYamlHandler yamlHandler;
  @InjectMocks @Inject private PipelineStageYamlHandler pipelineStageYamlHandler;
  @InjectMocks @Inject private SSHKeyDataProvider sshKeyDataProvider;
  @InjectMocks @Inject private WinRmConnectionAttributesDataProvider winRmConnectionAttributesDataProvider;
  @InjectMocks @Inject private EntityVersionService entityVersionService;

  private final String APP_NAME = "app1";
  private final String PIPELINE_NAME = "pipeline1";
  private final String PIPELINE_ID = "pipeline1Id";
  private Pipeline pipeline;

  private String validYamlContentUserGroup = "harnessApiVersion: '1.0'\n"
      + "type: PIPELINE\n"
      + "description: pipeline description\n"
      + "pipelineStages:\n"
      + "- type: APPROVAL\n"
      + "  name: Approval 0\n"
      + "  parallel: false\n"
      + "  properties:\n"
      + "    userGroups:\n"
      + "    - Account Administrator\n"
      + "    - Non-Production Support\n"
      + "    variables: null\n"
      + "    stageName: STAGE 2\n"
      + "    templateExpressions: null\n"
      + "    timeoutMillis: 86400000\n"
      + "    approvalStateType: USER_GROUP\n"
      + "    sweepingOutputName: ''\n"
      + "  skipCondition:\n"
      + "    type: DO_NOT_SKIP";

  private String validYamlContentUserGroupTemplatized = "harnessApiVersion: '1.0'\n"
      + "type: PIPELINE\n"
      + "description: pipeline description\n"
      + "pipelineStages:\n"
      + "- type: APPROVAL\n"
      + "  name: Approval 0\n"
      + "  parallel: false\n"
      + "  properties:\n"
      + "    userGroups: null\n"
      + "    variables: null\n"
      + "    stageName: STAGE 2\n"
      + "    templateExpressions:\n"
      + "    - expression: ${User_Group}\n"
      + "      expressionAllowed: true\n"
      + "      fieldName: USER_GROUP\n"
      + "      mandatory: false\n"
      + "      metadata:\n"
      + "        relatedField: ''\n"
      + "        entityType: USER_GROUP\n"
      + "    timeoutMillis: 86400000\n"
      + "    approvalStateType: USER_GROUP\n"
      + "    sweepingOutputName: ''\n"
      + "  skipCondition:\n"
      + "    type: DO_NOT_SKIP";

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
    when(userGroupService.fetchUserGroupByName(any(), eq("Non-Production Support"))).thenReturn(userGroup2);
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
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDAndGetUserGroup() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateType", "USER_GROUP");
    properties.put("variables", null);
    properties.put("stageName", "STAGE 2");
    properties.put("templateExpressions", null);
    properties.put("timeoutMillis", 86400000);
    properties.put("pipelineStageParallelIndex", 1);
    properties.put("disable", false);
    properties.put("sweepingOutputName", "");
    List<String> usergroups = new ArrayList<>();
    usergroups.add("Account Administrator");
    usergroups.add("Non-Production Support");
    properties.put("userGroups", usergroups);
    properties.put("pipelineStageElementId", "sbAge5a4SQWMJyPnBil89w");
    properties.put("pipelineId", "VPNAFmeAT-OBxnIGKn4Ckw");
    PipelineStageElement pipelineStage1 = PipelineStageElement.builder()
                                              .name("Approval 0")
                                              .type(StateType.APPROVAL.name())
                                              .properties(properties)
                                              .build();

    pipeline = Pipeline.builder()
                   .appId(APP_ID)
                   .name(PIPELINE_NAME)
                   .uuid(PIPELINE_ID)
                   .description("pipeline description")
                   .pipelineStages(Arrays.asList(
                       PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStage1)).build()))
                   .build();

    doReturn(pipeline).when(pipelineService).save(any());

    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContentUserGroup);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PIPELINE);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContentUserGroup, Yaml.class);
    changeContext.setYaml(yamlObject);

    Pipeline savedPipeline = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    comparePipeline(pipeline, savedPipeline);

    Yaml yaml = yamlHandler.toYaml(savedPipeline, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isNotNull();

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(validYamlContentUserGroup);

    yamlHandler.delete(changeContext);

    Pipeline pipeline = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(pipeline).isNull();
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testCRUDAndGetUserGroupTemplatized() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateType", "USER_GROUP");
    properties.put("variables", null);
    properties.put("stageName", "STAGE 2");
    properties.put("timeoutMillis", 86400000);
    properties.put("pipelineStageParallelIndex", 1);
    properties.put("disable", false);
    properties.put("sweepingOutputName", "");
    List<TemplateExpression> templateExpressions = new ArrayList<>();
    TemplateExpression t = new TemplateExpression();
    t.setFieldName("USER_GROUP");
    t.setExpression("${User_Group}");
    Map<String, Object> mData = new HashMap<>();
    mData.put("entityType", "USER_GROUP");
    mData.put("relatedField", "");
    t.setMetadata(mData);
    templateExpressions.add(t);
    properties.put("userGroups", null);
    properties.put("pipelineStageElementId", "sbAge5a4SQWMJyPnBil89w");
    properties.put("pipelineId", "VPNAFmeAT-OBxnIGKn4Ckw");
    properties.put("templateExpressions", templateExpressions);
    PipelineStageElement pipelineStage1 = PipelineStageElement.builder()
                                              .name("Approval 0")
                                              .type(StateType.APPROVAL.name())
                                              .properties(properties)
                                              .build();

    pipeline = Pipeline.builder()
                   .appId(APP_ID)
                   .name(PIPELINE_NAME)
                   .uuid(PIPELINE_ID)
                   .description("pipeline description")
                   .pipelineStages(Arrays.asList(
                       PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStage1)).build()))
                   .build();

    doReturn(pipeline).when(pipelineService).save(any());

    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(validYamlContentUserGroupTemplatized);
    gitFileChange.setFilePath(validYamlFilePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.PIPELINE);
    changeContext.setYamlSyncHandler(yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContentUserGroupTemplatized, Yaml.class);
    changeContext.setYaml(yamlObject);

    Pipeline savedPipeline = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    comparePipeline(pipeline, savedPipeline);

    Yaml yaml = yamlHandler.toYaml(savedPipeline, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isNotNull();

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(validYamlContentUserGroupTemplatized);

    yamlHandler.delete(changeContext);

    Pipeline pipeline = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(pipeline).isNull();
  }

  private void comparePipeline(Pipeline lhs, Pipeline rhs) {
    assertThat(rhs.getDescription()).isEqualTo(lhs.getDescription());
  }
}
