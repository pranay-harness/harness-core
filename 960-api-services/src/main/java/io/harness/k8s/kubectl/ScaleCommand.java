package io.harness.k8s.kubectl;

import org.apache.commons.lang3.StringUtils;

public class ScaleCommand extends AbstractExecutable {
  private Kubectl client;
  private String resource;
  private String namespace;
  private int replicas;

  public ScaleCommand(Kubectl client) {
    this.client = client;
  }

  public ScaleCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public ScaleCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public ScaleCommand replicas(int replicas) {
    this.replicas = replicas;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("scale ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    command.append(Kubectl.option(Option.replicas, this.replicas));

    return command.toString().trim();
  }
}
