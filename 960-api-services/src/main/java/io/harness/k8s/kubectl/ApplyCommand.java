package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import org.apache.commons.lang3.StringUtils;

public class ApplyCommand extends AbstractExecutable {
  private Kubectl client;
  private String filename;
  private String namespace;
  private boolean dryrun;
  private boolean record;
  private String output;

  public ApplyCommand(Kubectl client) {
    this.client = client;
  }

  public ApplyCommand filename(String filename) {
    this.filename = filename;
    return this;
  }

  public ApplyCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public ApplyCommand dryrun(boolean dryrun) {
    this.dryrun = dryrun;
    return this;
  }

  public ApplyCommand record(boolean record) {
    this.record = record;
    return this;
  }

  public ApplyCommand output(String output) {
    this.output = output;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("apply ");

    if (StringUtils.isNotBlank(this.filename)) {
      command.append(Kubectl.option(Option.filename, encloseWithQuotesIfNeeded(this.filename)));
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    if (this.dryrun) {
      command.append(Kubectl.flag(Flag.dryrun));
    }

    if (this.record) {
      command.append(Kubectl.flag(Flag.record));
    }

    if (this.output != null) {
      command.append(Kubectl.option(Option.output, output));
    }

    return command.toString().trim();
  }
}
