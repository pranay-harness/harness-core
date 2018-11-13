package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class RolloutHistoryCommand extends AbstractExecutable {
  private RolloutCommand rolloutCommand;
  private String resource;
  private String namespace;

  public RolloutHistoryCommand(RolloutCommand rolloutCommand) {
    this.rolloutCommand = rolloutCommand;
  }

  public RolloutHistoryCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public RolloutHistoryCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(rolloutCommand.command()).append("history ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    return command.toString().trim();
  }
}
