package software.wings.sm.states;

import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.resume.ResumeStateUtils;

import java.util.List;

@Attributes(title = "EnvResume")
@Slf4j
@FieldNameConstants(innerTypeName = "EnvResumeStateKeys")
public class EnvResumeState extends State {
  // This is guaranteed to contain EnvStateExecutionData.
  @Setter @SchemaIgnore private String prevStateExecutionId;
  @Setter @SchemaIgnore private String prevPipelineExecutionId;
  @Setter @SchemaIgnore private List<String> prevWorkflowExecutionIds;

  @Transient @Inject private ResumeStateUtils resumeStateUtils;

  public EnvResumeState(String name) {
    super(name, StateType.ENV_RESUME_STATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = context.getAppId();
    String currPipelineExecutionId = resumeStateUtils.fetchPipelineExecutionId(context);
    notNullCheck("Pipeline execution is null in EnvResumeState", currPipelineExecutionId);
    resumeStateUtils.copyPipelineStageOutputs(appId, prevPipelineExecutionId, prevStateExecutionId,
        prevWorkflowExecutionIds, currPipelineExecutionId, context.getStateExecutionInstanceId());
    return resumeStateUtils.prepareExecutionResponse(context, prevStateExecutionId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Not doing anything on abort.
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    Integer timeout = super.getTimeoutMillis();
    return timeout == null ? ResumeStateUtils.RESUME_STATE_TIMEOUT_MILLIS : timeout;
  }
}
