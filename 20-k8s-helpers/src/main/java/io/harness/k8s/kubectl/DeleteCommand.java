package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class DeleteCommand extends AbstractExecutable {
  private Kubectl client;
  private String filename;
  private String resources;
  private String namespace;

  public DeleteCommand(Kubectl client) {
    this.client = client;
  }

  public DeleteCommand filename(String filename) {
    this.filename = filename;
    return this;
  }

  public DeleteCommand resources(String resources) {
    this.resources = resources;
    return this;
  }

  public DeleteCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("delete ");

    if (StringUtils.isNotBlank(this.resources)) {
      command.append(this.resources).append(' ');
    }

    if (StringUtils.isNotBlank(this.filename)) {
      command.append(Kubectl.option(Option.filename, this.filename));
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    return command.toString().trim();
  }
}
