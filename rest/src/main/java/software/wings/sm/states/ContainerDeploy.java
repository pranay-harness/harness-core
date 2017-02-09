package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by rishi on 2/8/17.
 */
public class ContainerDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(ContainerDeploy.class);

  @Attributes(title = "Number of instances") private int instanceCount;

  public ContainerDeploy(String name) {
    super(name, StateType.HTTP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return anExecutionResponse().build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
