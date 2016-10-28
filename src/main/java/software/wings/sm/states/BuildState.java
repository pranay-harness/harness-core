package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.BuildStateExecutionData;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class BuildState extends State {
  @EnumData(enumDataProvider = ArtifactStreamServiceImpl.class)
  @Attributes(required = true, title = "Artifact Stream")
  private String artifactStreamId;

  @Transient @Inject private ArtifactService artifactService;
  @Transient @Inject private PipelineService pipelineService;
  @Transient @Inject private ExecutorService executorService;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public BuildState(String name) {
    super(name, StateType.BUILD.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = context.getApp().getUuid();
    String pipelineExecutionId = context.getWorkflowExecutionId();
    BuildStateExecutionData buildStateExecutionData = new BuildStateExecutionData();
    buildStateExecutionData.setArtifactStreamId(artifactStreamId);
    Artifact artifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);

    if (artifact == null) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("No artifact found for deployment")
          .build();
    }
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    workflowStandardParams.setArtifactIds(asList(artifact.getUuid()));
    executorService.submit(() -> pipelineService.updatePipelineExecutionData(appId, pipelineExecutionId, artifact));
    return anExecutionResponse().withStateExecutionData(buildStateExecutionData).build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }
}
