/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import org.apache.commons.lang3.StringUtils;

public class GetCommand extends AbstractExecutable {
  private Kubectl client;
  private String filename;
  private String resources;
  private String namespace;
  private String output;
  private boolean watch;
  private boolean watchOnly;
  private boolean export;
  private boolean allNamespaces;

  public GetCommand(Kubectl client) {
    this.client = client;
  }

  public GetCommand filename(String filename) {
    this.filename = filename;
    return this;
  }

  public GetCommand resources(String resources) {
    this.resources = resources;
    return this;
  }

  public GetCommand namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public GetCommand output(String output) {
    this.output = output;
    return this;
  }

  public GetCommand watch(boolean watch) {
    this.watch = watch;
    return this;
  }

  public GetCommand watchOnly(boolean watchOnly) {
    this.watchOnly = watchOnly;
    return this;
  }

  public GetCommand export(boolean export) {
    this.export = export;
    return this;
  }

  public GetCommand allNamespaces(boolean allNamespaces) {
    this.allNamespaces = allNamespaces;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("get ");

    if (StringUtils.isNotBlank(this.resources)) {
      command.append(this.resources).append(' ');
    }

    if (StringUtils.isNotBlank(this.filename)) {
      command.append(Kubectl.option(Option.filename, encloseWithQuotesIfNeeded(this.filename)));
    }

    if (StringUtils.isNotBlank(this.namespace)) {
      command.append(Kubectl.option(Option.namespace, this.namespace));
    }

    if (this.allNamespaces) {
      command.append(Kubectl.option(Option.allNamespaces, "true"));
    }

    if (this.output != null) {
      command.append(Kubectl.option(Option.output, output));
    }

    if (this.watch) {
      command.append(Kubectl.flag(Flag.watch));
    }

    if (this.watchOnly) {
      command.append(Kubectl.flag(Flag.watchOnly));
    }

    if (this.export) {
      command.append(Kubectl.flag(Flag.export));
    }

    return command.toString().trim();
  }
}
