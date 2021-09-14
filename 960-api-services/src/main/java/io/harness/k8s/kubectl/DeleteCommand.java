/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

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

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("delete ");

    if (StringUtils.isNotBlank(this.resources)) {
      command.append(this.resources).append(' ');
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
