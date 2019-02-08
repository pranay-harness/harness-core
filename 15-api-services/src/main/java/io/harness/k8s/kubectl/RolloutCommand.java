package io.harness.k8s.kubectl;

public class RolloutCommand extends AbstractExecutable {
  private Kubectl client;

  public RolloutCommand(Kubectl client) {
    this.client = client;
  }

  public RolloutStatusCommand status() {
    return new RolloutStatusCommand(this);
  }

  public RolloutHistoryCommand history() {
    return new RolloutHistoryCommand(this);
  }

  public RolloutUndoCommand undo() {
    return new RolloutUndoCommand(this);
  }

  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("rollout ");

    return command.toString();
  }
}
