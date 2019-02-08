package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class RolloutUndoCommand extends AbstractExecutable {
  private RolloutCommand rolloutCommand;
  private String resource;
  private String namespace;
  private String toRevision;

  public RolloutUndoCommand(RolloutCommand rolloutCommand) {
    this.rolloutCommand = rolloutCommand;
  }

  public RolloutUndoCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public RolloutUndoCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public RolloutUndoCommand toRevision(String toRevision) {
    this.toRevision = toRevision;
    return this;
  }

  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(rolloutCommand.command()).append("undo ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    if (StringUtils.isNotBlank(this.toRevision)) {
      command.append(Kubectl.option(Option.toRevision, this.toRevision));
    }

    return command.toString().trim();
  }
}
