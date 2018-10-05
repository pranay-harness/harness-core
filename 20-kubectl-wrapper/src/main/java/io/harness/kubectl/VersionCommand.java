package io.harness.kubectl;

public class VersionCommand extends AbstractExecutable {
  public VersionCommand(Kubectl client) {
    this.client = client;
  }

  Kubectl client;

  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("version");

    return command.toString();
  }
}
