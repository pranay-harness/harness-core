package software.wings.service.impl.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

public class ArtifactConditionTriggerTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private ArtifactTriggerProcessor artifactTriggerProcessor;

  @Inject private DeploymentTriggerGenerator deploymentTriggerGenerator;

  @Before
  public void setUp() {
    when(pipelineService.fetchPipelineName(APP_ID, PIPELINE_ID)).thenReturn(PIPELINE_NAME);
    when(workflowService.fetchWorkflowName(APP_ID, WORKFLOW_ID)).thenReturn(WORKFLOW_NAME);
  }
  @Test
  public void shouldCRUDArtifactConditionTrigger() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(JenkinsArtifactStream.builder()
                        .uuid(ARTIFACT_STREAM_ID)
                        .serviceId(SERVICE_ID)
                        .sourceName(ARTIFACT_SOURCE_NAME)
                        .build());

    when(serviceResourceService.fetchServiceName(APP_ID, SERVICE_ID)).thenReturn(SERVICE_NAME);

    DeploymentTrigger trigger = DeploymentTrigger.builder()
                                    .name("New Artifact Pipeline")
                                    .workflowId(PIPELINE_ID)
                                    .condition(ArtifactCondition.builder().build())
                                    .build();

    DeploymentTrigger savedTrigger =
        deploymentTriggerService.save(deploymentTriggerGenerator.ensureDeploymentTrigger(trigger));

    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getWorkflowName()).isEqualTo(PIPELINE_NAME);

    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactSourceName())
        .isNotNull()
        .isEqualTo(ARTIFACT_SOURCE_NAME + " (" + SERVICE_NAME + ")");

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

    verify(pipelineService, times(2)).fetchPipelineName(APP_ID, PIPELINE_ID);
  }
}