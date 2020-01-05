package io.harness.k8s.kubectl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GetPodCommand extends AbstractExecutable {
  private GetCommand getCommand;
  private String selector;
  private String output;

  public GetPodCommand(GetCommand getCommand) {
    this.getCommand = getCommand;
  }

  public GetPodCommand selector(String selector) {
    this.selector = selector;
    return this;
  }

  public GetPodCommand output(String output) {
    this.output = output;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(getCommand.command()).append(" " + ResourceType.pods.toString() + " ");

    if (isNotBlank(selector)) {
      command.append(Kubectl.option(Option.selector, selector));
    }

    if (isNotBlank(output)) {
      command.append(Kubectl.option(Option.output, output));
    }

    return command.toString().trim();
  }
}
