package software.wings.sm.states.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.command.CommandUnit;
import software.wings.sm.ExecutionContext;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class AbstractAzureState extends State {
  public AbstractAzureState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  public abstract List<CommandUnit> getCommandUnits(ExecutionContext context, ResizeStrategy resizeStrategy);
}
