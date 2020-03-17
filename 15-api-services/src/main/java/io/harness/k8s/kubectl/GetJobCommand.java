package io.harness.k8s.kubectl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GetJobCommand extends AbstractExecutable {
  private GetCommand getCommand;
  private String jobName;
  private String output;
  private String namespace;

  public GetJobCommand(GetCommand getCommand, String jobName, String namespace) {
    this.getCommand = getCommand;
    this.jobName = jobName;
    this.namespace = namespace;
  }

  public GetJobCommand output(String output) {
    this.output = output;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(getCommand.command())
        .append(' ')
        .append(ResourceType.jobs.toString())
        .append(' ')
        .append(jobName)
        .append(' ');

    if (isNotBlank(namespace)) {
      command.append(Kubectl.option(Option.namespace, namespace));
    }

    if (isNotBlank(output)) {
      command.append(Kubectl.option(Option.output, output));
    }

    return command.toString().trim();
  }
}
