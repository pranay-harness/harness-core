package software.wings.service.impl.trigger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildJenkinsArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildScheduledCondDeploymentTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflow;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.distribution.idempotence.IdempotentLock;
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
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.service.impl.trigger.TriggerServiceImpl.TriggerIdempotentResult;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.List;
import java.util.Optional;

public class ScheduleConditionTriggerTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private ServiceVariableService serviceVariablesService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactService artifactService;
  @Mock private MongoIdempotentRegistry<TriggerIdempotentResult> idempotentRegistry;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private DeploymentTriggerServiceHelper deploymentTriggerServiceHelper;
  @Inject @InjectMocks private TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject @InjectMocks private ScheduleTriggerProcessor scheduleTriggerProcessor;

  DeploymentTrigger scheduledConditionTrigger = buildScheduledCondDeploymentTrigger();
  JenkinsArtifactStream jenkinsArtifactStream = buildJenkinsArtifactStream();

  @Before
  public void setUp() {
    List<ServiceVariable> serviceVariableList = asList(
        ServiceVariable.builder().type(Type.ARTIFACT).name(VARIABLE_NAME).value(VARIABLE_VALUE.toCharArray()).build());
    Pipeline pipeline = buildPipeline();
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, true))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, false)).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(serviceResourceService.get(ENTITY_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());

    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactStreamServiceBindingService.listServiceIds(ARTIFACT_STREAM_ID)).thenReturn(asList(SERVICE_ID));

    when(idempotentRegistry.create(any(), any(), any(), any()))
        .thenReturn(IdempotentLock.<TriggerIdempotentResult>builder()
                        .registry(idempotentRegistry)
                        .resultData(Optional.empty())
                        .build());
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    on(triggerArtifactVariableHandler).set("serviceVariablesService", serviceVariablesService);
    when(serviceVariablesService.getServiceVariablesForEntity(APP_ID, ENTITY_ID, OBTAIN_VALUE))
        .thenReturn(serviceVariableList);
  }
  @Test
  @Category(UnitTests.class)
  public void shouldSaveScheduledConditionTrigger() {
    DeploymentTrigger trigger = deploymentTriggerService.save(scheduledConditionTrigger);
    DeploymentTrigger savedScheduledTrigger = deploymentTriggerService.get(trigger.getAppId(), trigger.getUuid());

    assertThat(savedScheduledTrigger.getUuid()).isNotEmpty();
    assertThat(savedScheduledTrigger.getCondition()).isInstanceOf(ScheduledCondition.class);
    assertThat(((ScheduledCondition) trigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledCondition) trigger.getCondition()).getCronExpression()).isNotNull().isEqualTo("* * * * ?");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateScheduledConditionTrigger() {
    scheduledConditionTrigger = deploymentTriggerService.save(scheduledConditionTrigger);
    scheduledConditionTrigger.setDescription("updated description");

    DeploymentTrigger updatedTrigger = deploymentTriggerService.update(scheduledConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);

    assertThat(updatedTrigger.getCondition()).isInstanceOf(ScheduledCondition.class);
    assertThat(((ScheduledCondition) updatedTrigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledCondition) updatedTrigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    assertThat(updatedTrigger.getDescription()).isNotNull().isEqualTo("updated description");
  }
}
