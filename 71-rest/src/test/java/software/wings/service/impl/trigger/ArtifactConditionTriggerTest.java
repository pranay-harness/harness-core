package software.wings.service.impl.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_DESCRIPTION;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

public class ArtifactConditionTriggerTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private ArtifactTriggerProcessor artifactTriggerProcessor;

  @Inject private DeploymentTriggerGenerator deploymentTriggerGenerator;

  @Test
  @Category(UnitTests.class)
  public void shouldCRUDArtifactConditionTrigger() {
    when(artifactStreamService.get(ARTIFACT_STREAM_ID))
        .thenReturn(JenkinsArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .serviceId(SERVICE_ID)
                        .sourceName(ARTIFACT_SOURCE_NAME)
                        .build());

    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).build());

    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .name("New Artifact Pipeline")
            .action(PipelineAction.builder().pipelineId(PIPELINE_ID).triggerArgs(TriggerArgs.builder().build()).build())
            .condition(ArtifactCondition.builder().artifactStreamId(ARTIFACT_STREAM_ID).build())
            .build();

    DeploymentTrigger savedTrigger =
        deploymentTriggerService.save(deploymentTriggerGenerator.ensureDeploymentTrigger(trigger));

    assertThat(savedTrigger).isNotNull();
    PipelineAction pipelineAction = (PipelineAction) (savedTrigger.getAction());
    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactSourceName())
        .isNotNull()
        .isEqualTo(ARTIFACT_SOURCE_NAME);

    savedTrigger = deploymentTriggerService.get(savedTrigger.getAppId(), savedTrigger.getUuid());

    assertThat(savedTrigger).isNotNull();
    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    savedTrigger.setDescription(TRIGGER_DESCRIPTION);

    DeploymentTrigger updatedTrigger = deploymentTriggerService.update(savedTrigger);

    assertThat(updatedTrigger).isNotNull();
    assertThat(updatedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());
    assertThat(updatedTrigger.getDescription()).isEqualTo(TRIGGER_DESCRIPTION);
  }
}