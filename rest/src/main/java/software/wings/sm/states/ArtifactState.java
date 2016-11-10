package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by anubhaw on 11/4/16.
 */
public class ArtifactState extends State {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public ArtifactState(String name) {
    super(name, StateType.ARTIFACT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return new ExecutionResponse();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
