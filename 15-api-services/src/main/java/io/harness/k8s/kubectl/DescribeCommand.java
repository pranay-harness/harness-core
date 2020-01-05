package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import org.apache.commons.lang3.StringUtils;

public class DescribeCommand extends AbstractExecutable {
  private Kubectl client;
  private String resource;
  private String filename;
  private String namespace;

  public DescribeCommand(Kubectl client) {
    this.client = client;
  }

  public DescribeCommand resource(String resource) {
    this.resource = resource;
    return this;
  }

  public DescribeCommand filename(String filename) {
    this.filename = filename;
    return this;
  }

  public DescribeCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(this.client.command()).append("describe ");

    if (StringUtils.isNotBlank(this.resource)) {
      command.append(this.resource).append(' ');
    }

    if (StringUtils.isNotBlank(this.filename)) {
      command.append(Kubectl.option(Option.filename, encloseWithQuotesIfNeeded(this.filename)));
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    return command.toString().trim();
  }
}
