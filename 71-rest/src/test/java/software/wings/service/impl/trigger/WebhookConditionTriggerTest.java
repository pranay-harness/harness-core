package software.wings.service.impl.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildJenkinsArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflow;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.getCustomCondition;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.getPipelineAction;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.getWorkflowAction;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACTORY_URL;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.trigger.CustomPayloadSource;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.List;

public class WebhookConditionTriggerTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;
  @Mock private ServiceVariableService serviceVariablesService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private DeploymentTriggerServiceHelper deploymentTriggerServiceHelper;
  @Inject @InjectMocks private TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject @InjectMocks private WebhookTriggerProcessor webhookTriggerProcessor;

  JenkinsArtifactStream jenkinsArtifactStream = buildJenkinsArtifactStream();
  @Before
  public void setUp() {
    SettingAttribute artifactorySetting = aSettingAttribute()
                                              .withUuid(SETTING_ID)
                                              .withName(SETTING_NAME)
                                              .withValue(ArtifactoryConfig.builder()
                                                             .artifactoryUrl(ARTIFACTORY_URL)
                                                             .username("admin")
                                                             .password("dummy123!".toCharArray())
                                                             .build())
                                              .build();
    Pipeline pipeline = buildPipeline();
    Workflow workflow = buildWorkflow();
    List<ServiceVariable> serviceVariableList = asList(
        ServiceVariable.builder().type(Type.ARTIFACT).name(VARIABLE_NAME).value(VARIABLE_VALUE.toCharArray()).build());
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(settingsService.get(SETTING_ID)).thenReturn(artifactorySetting);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, true))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    on(triggerArtifactVariableHandler).set("serviceVariablesService", serviceVariablesService);
    when(serviceVariablesService.getServiceVariablesForEntity(APP_ID, ENTITY_ID, OBTAIN_VALUE))
        .thenReturn(serviceVariableList);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(serviceResourceService.get(ENTITY_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldSaveWebhookConditionTrigger() {
    DeploymentTrigger trigger =
        deploymentTriggerService.save(TriggerServiceTestHelper.buildWebhookConditionTrigger(), false);
    DeploymentTrigger savedWebhookTrigger = deploymentTriggerService.get(trigger.getAppId(), trigger.getUuid(), false);

    assertThat(savedWebhookTrigger.getUuid()).isNotEmpty();
    assertThat(savedWebhookTrigger.getCondition()).isInstanceOf(WebhookCondition.class);
    assertThat(((WebhookCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebhookCondition) trigger.getCondition()).getPayloadSource()).isNotNull();

    GitHubPayloadSource gitHubPayloadSource =
        (GitHubPayloadSource) ((WebhookCondition) trigger.getCondition()).getPayloadSource();

    assertThat(gitHubPayloadSource.getGitHubEventTypes().equals(asList(GitHubEventType.PUSH)));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldSaveWebhookConditionTriggerWithExpressions() {
    DeploymentTrigger trigger = TriggerServiceTestHelper.buildWebhookConditionTrigger();

    GitHubPayloadSource gitHubPayloadSource =
        (GitHubPayloadSource) ((WebhookCondition) trigger.getCondition()).getPayloadSource();

    trigger.setAction(getPipelineAction());
    deploymentTriggerService.save(trigger, false);

    DeploymentTrigger savedWebhookTrigger = deploymentTriggerService.get(trigger.getAppId(), trigger.getUuid(), false);

    assertThat(savedWebhookTrigger.getUuid()).isNotEmpty();
    assertThat(savedWebhookTrigger.getCondition()).isInstanceOf(WebhookCondition.class);
    assertThat(((WebhookCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebhookCondition) trigger.getCondition()).getPayloadSource()).isNotNull();

    assertThat(gitHubPayloadSource.getGitHubEventTypes().equals(asList(GitHubEventType.PUSH))).isTrue();

    trigger.setAction(getWorkflowAction());

    deploymentTriggerService.save(trigger, false);
    assertThat(savedWebhookTrigger.getUuid()).isNotEmpty();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldUpdateWebhookConditionTrigger() {
    DeploymentTrigger trigger =
        deploymentTriggerService.save(TriggerServiceTestHelper.buildWebhookConditionTrigger(), false);
    trigger.setDescription("updated description");

    DeploymentTrigger updatedTrigger = deploymentTriggerService.update(trigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);

    assertThat(updatedTrigger.getCondition()).isInstanceOf(WebhookCondition.class);
    assertThat((WebhookCondition) updatedTrigger.getCondition()).isNotNull();
    GitHubPayloadSource gitHubPayloadSource =
        (GitHubPayloadSource) ((WebhookCondition) trigger.getCondition()).getPayloadSource();

    assertThat(gitHubPayloadSource.getGitHubEventTypes().equals(asList(GitHubEventType.PUSH)));
    assertThat(updatedTrigger.getDescription()).isNotNull().isEqualTo("updated description");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldSaveCustomWebhookConditionTrigger() {
    DeploymentTrigger trigger = TriggerServiceTestHelper.buildWebhookConditionTrigger();
    trigger.setCondition(getCustomCondition());
    DeploymentTrigger savedTrigger = deploymentTriggerService.save(trigger, false);

    assertThat(savedTrigger.getCondition()).isInstanceOf(WebhookCondition.class);
    assertThat((WebhookCondition) savedTrigger.getCondition()).isNotNull();

    assertThat(((WebhookCondition) savedTrigger.getCondition()).getPayloadSource())
        .isInstanceOf(CustomPayloadSource.class);
  }
}
