package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;

import static software.wings.sm.StateType.RANCHER_RESOLVE;

public class RancherResolveState extends State {

    private String commandName = "Rancher Resolve";
    public RancherResolveState(String name) {
        super(name, RANCHER_RESOLVE.name());
    }

    @Override
    public ExecutionResponse execute(ExecutionContext context) {
        return null;
    }

    @Override
    public void handleAbortEvent(ExecutionContext context) {

    }

    public String getCommandName() {
        return this.commandName;
    }

}
