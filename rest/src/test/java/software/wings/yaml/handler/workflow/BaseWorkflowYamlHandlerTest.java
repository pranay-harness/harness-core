package software.wings.yaml.handler.workflow;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.api.DeploymentType;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.FailureStrategyYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.PhaseStepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.StepYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowPhaseYamlHandler;
import software.wings.service.impl.yaml.handler.workflow.WorkflowYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Optional;

/**
 * @author rktummala on 1/11/18
 */
public abstract class BaseWorkflowYamlHandlerTest extends BaseYamlHandlerTest {
  protected InfrastructureMapping infrastructureMapping = getInfraMapping();
  protected Service service = getService();
  protected Environment environment = getEnvironment();
  protected ArtifactStream artifactStream = getArtifactStream();
  protected NotificationGroup notificationGroup = getNotificationGroup();

  @Mock protected YamlHelper yamlHelper;
  @Mock protected YamlHandlerFactory yamlHandlerFactory;
  @Mock protected EnvironmentService environmentService;

  @Mock protected AppService appService;
  @Mock protected InfrastructureMappingService infrastructureMappingService;
  @Mock protected ServiceResourceService serviceResourceService;
  @Mock protected ArtifactStreamService artifactStreamService;
  @Mock protected SettingsService settingsService;
  @Mock protected NotificationSetupService notificationSetupService;

  //  @InjectMocks @Inject YamlHelper yamlHelper;
  @InjectMocks @Inject protected WorkflowService workflowService;
  @InjectMocks @Inject protected WorkflowPhaseYamlHandler phaseYamlHandler;
  @InjectMocks @Inject protected PhaseStepYamlHandler phaseStepYamlHandler;
  @InjectMocks @Inject protected StepYamlHandler stepYamlHandler;
  @InjectMocks @Inject protected FailureStrategyYamlHandler failureStrategyYamlHandler;
  @InjectMocks @Inject protected NotificationRulesYamlHandler notificationRulesYamlHandler;
  @InjectMocks @Inject protected TemplateExpressionYamlHandler templateExpressionYamlHandler;
  @InjectMocks @Inject protected VariableYamlHandler variableYamlHandler;
  @InjectMocks @Inject protected NameValuePairYamlHandler nameValuePairYamlHandler;
  @InjectMocks @Inject protected WorkflowServiceHelper workflowServiceHelper;

  protected void setup(String yamlFilePath, String workflowName) {
    when(appService.getAppByName(anyString(), anyString()))
        .thenReturn(anApplication().withName(APP_NAME).withUuid(APP_ID).build());

    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(APP_ID);
    when(yamlHelper.getApplicationIfPresent(anyString(), anyString()))
        .thenReturn(Optional.of(anApplication().withUuid(APP_ID).build()));
    when(yamlHelper.getNameFromYamlFilePath(yamlFilePath)).thenReturn(workflowName);
    when(yamlHelper.extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER))
        .thenReturn(workflowName);

    when(artifactStreamService.getArtifactStreamByName(anyString(), anyString(), anyString()))
        .thenReturn(artifactStream);
    when(artifactStreamService.get(anyString(), anyString())).thenReturn(artifactStream);

    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(environment);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(environment);
    when(environmentService.get(APP_ID, ENV_ID)).thenReturn(environment);

    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);

    when(notificationSetupService.readNotificationGroupByName(anyString(), anyString())).thenReturn(notificationGroup);
    when(notificationSetupService.readNotificationGroup(anyString(), anyString())).thenReturn(notificationGroup);

    when(infrastructureMappingService.getInfraMappingByName(anyString(), anyString(), anyString()))
        .thenReturn(infrastructureMapping);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    when(yamlHandlerFactory.getYamlHandler(YamlType.PHASE)).thenReturn(phaseYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.PHASE_STEP)).thenReturn(phaseStepYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION)).thenReturn(templateExpressionYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.STEP)).thenReturn(stepYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY)).thenReturn(failureStrategyYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE)).thenReturn(notificationRulesYamlHandler);

    when(yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE)).thenReturn(variableYamlHandler);
    when(yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR)).thenReturn(nameValuePairYamlHandler);
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
    return Service.builder()
        .name(SERVICE_NAME)
        .appId(APP_ID)
        .uuid(SERVICE_ID)
        .artifactType(ArtifactType.DOCKER)
        .build();
  }

  private Environment getEnvironment() {
    return Environment.Builder.anEnvironment().withName(ENV_NAME).withAppId(APP_ID).withUuid(ENV_ID).build();
  }

  private ArtifactStream getArtifactStream() {
    return GcrArtifactStream.builder()
        .appId(APP_ID)
        .serviceId(SERVICE_ID)
        .dockerImageName("testDockerImageName")
        .sourceName("gcr.io_exploration-161417_todolist")
        .name("gcr.io_exploration-161417_todolist")
        .build();
  }

  private NotificationGroup getNotificationGroup() {
    return aNotificationGroup()
        .withUuid(NOTIFICATION_GROUP_ID)
        .withName("Account Administrator")
        .withAccountId(ACCOUNT_ID)
        .build();
  }

  protected <Y extends BaseYaml, H extends BaseYamlHandler> ChangeContext<Y> getChangeContext(
      String yamlContent, String yamlFilePath, H yamlHandler) {
    // Invalid yaml path
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(yamlContent)
                                      .build();

    ChangeContext<Y> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.WORKFLOW);
    changeContext.setYamlSyncHandler(yamlHandler);
    return changeContext;
  }

  protected <Y extends BaseYaml, H extends WorkflowYamlHandler> void testFailures(String validYamlContent,
      String validYamlFilePath, String invalidYamlContent, String invalidYamlFilePath, H yamlHandler,
      Class<Y> yamlClass) throws HarnessException, IOException {
    ChangeContext<Y> changeContext = getChangeContext(validYamlContent, invalidYamlFilePath, yamlHandler);

    Y yamlObject = (Y) getYaml(validYamlContent, yamlClass);
    changeContext.setYaml(yamlObject);

    thrown.expect(Exception.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));

    // Invalid yaml content
    changeContext = getChangeContext(invalidYamlContent, validYamlFilePath, yamlHandler);

    yamlObject = (Y) getYaml(invalidYamlContent, yamlClass);
    changeContext.setYaml(yamlObject);

    thrown.expect(WingsException.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }
}
